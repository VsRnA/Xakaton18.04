package com.prodforge.game.infrastructure.rng;

import com.prodforge.game.domain.rng.RngCommitment;
import com.prodforge.game.domain.rng.RngPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RngServiceImpl implements RngPort {

    private static final int WEIGHT_MIN = -10;
    private static final int WEIGHT_MAX = 10;
    // Диапазон значений весов: 21 вариант (-10..0..+10)
    private static final int WEIGHT_RANGE_SIZE = WEIGHT_MAX - WEIGHT_MIN + 1;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Фаза COMMIT: генерирует seed до начала раунда и публикует его хэш игрокам.
     * Игроки видят только хэш — сам seed скрыт. Это гарантирует,
     * что веса нельзя подобрать заранее, но можно проверить после раскрытия.
     */
    @Override
    public RngCommitment commit(UUID roomId, int roundNumber) {
        byte[] seed = buildSeed(roomId, roundNumber);
        String seedHex = HexFormat.of().formatHex(seed);
        String seedHash = sha256Hex(seed);
        // seedHash публикуется игрокам; seedHex хранится в БД до фазы reveal
        return new RngCommitment(seedHex, seedHash);
    }

    /**
     * Фаза REVEAL: раскрывает seed и генерирует веса бочек.
     * Вызывается при завершении раунда — игроки могут сверить seedHex с ранее
     * опубликованным хэшем и убедиться, что результат не был подменён.
     */
    @Override
    public List<BigDecimal> reveal(String seedHex, int count) {
        byte[] seed = HexFormat.of().parseHex(seedHex);
        return generateWeights(seed, count);
    }

    /**
     * Строит 32-байтовый seed из трёх независимых источников энтропии,
     * объединённых побайтово через XOR:
     *   1. osEntropy   — 32 случайных байта от ОС (SecureRandom)
     *   2. timeEntropy — nanoTime + currentTimeMillis (16 байт, защита от предсказания по времени)
     *   3. ctxEntropy  — SHA-256("roomId:roundNumber") (детерминированный контекст комнаты/раунда)
     * XOR трёх источников: даже если один скомпрометирован, seed остаётся непредсказуемым.
     */
    private byte[] buildSeed(UUID roomId, int roundNumber) {
        byte[] osEntropy = new byte[32];
        secureRandom.nextBytes(osEntropy);

        byte[] timeEntropy = longToBytes(System.nanoTime(), System.currentTimeMillis());

        String ctx = roomId.toString() + ":" + roundNumber;
        byte[] ctxEntropy = sha256(ctx.getBytes(StandardCharsets.UTF_8));

        byte[] seed = new byte[32];
        for (int byteIndex = 0; byteIndex < 32; byteIndex++) {
            seed[byteIndex] = (byte) (osEntropy[byteIndex] ^ timeEntropy[byteIndex % timeEntropy.length] ^ ctxEntropy[byteIndex]);
        }
        return seed;
    }

    /**
     * Генерирует веса бочек методом SHA-256 hash chain в режиме счётчика.
     *
     * Для каждой бочки с индексом i:
     *   current = SHA-256(previous_hash || i_as_4_bytes)
     *   raw     = первые 4 байта current, прочитанные как int
     *   weight  = floorMod(raw, 21) - 10  →  результат в [-10, +10]
     *
     * Цепочка гарантирует, что все 256 бит энтропии seed распространяются
     * на каждый вес — нет потерь от XOR-сжатия в long.
     */
    private List<BigDecimal> generateWeights(byte[] seed, int count) {
        List<BigDecimal> weights = new ArrayList<>(count);
        byte[] current = seed;
        for (int weightIndex = 0; weightIndex < count; weightIndex++) {
            // Хэшируем предыдущий хэш вместе с порядковым номером бочки
            current = sha256(concat(current, intToBytes(weightIndex)));
            int raw = readInt(current, 0);
            // floorMod даёт равномерное распределение по всем 21 значениям (в отличие от %)
            int intWeight = Math.floorMod(raw, WEIGHT_RANGE_SIZE) + WEIGHT_MIN;
            weights.add(BigDecimal.valueOf(intWeight));
        }
        return weights;
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }

    private int readInt(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 24) | ((buf[offset + 1] & 0xFF) << 16)
                | ((buf[offset + 2] & 0xFF) << 8) | (buf[offset + 3] & 0xFF);
    }

    private byte[] longToBytes(long highBits, long lowBits) {
        byte[] buf = new byte[16];
        for (int byteIndex = 7; byteIndex >= 0; byteIndex--) {
            buf[byteIndex] = (byte) (highBits & 0xFF);
            buf[byteIndex + 8] = (byte) (lowBits & 0xFF);
            highBits >>= 8;
            lowBits >>= 8;
        }
        return buf;
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String sha256Hex(byte[] input) {
        return HexFormat.of().formatHex(sha256(input));
    }
}
