package com.vsrna.game.infrastructure.rng;

import com.vsrna.game.domain.rng.RngCommitment;
import com.vsrna.game.domain.rng.RngPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class RngServiceImpl implements RngPort {

    /** Веса бочек: целые числа от -10 до 10 включительно (21 значение). */
    private static final int WEIGHT_MIN = -10;
    private static final int WEIGHT_RANGE_SIZE = 21; // [-10, 10]

    private final SecureRandom secureRandom = new SecureRandom();

    /** Фаза 1: генерируем seed, возвращаем seedHash (публикуется) + rawSeed (хранится в БД). */
    @Override
    public RngCommitment commit(UUID roomId, int roundNumber) {
        byte[] seed = buildSeed(roomId, roundNumber);
        String seedHex = HexFormat.of().formatHex(seed);
        String seedHash = sha256Hex(seed);
        return new RngCommitment(seedHex, seedHash);
    }

    /** Фаза 2: детерминированно восстанавливаем веса из сохранённого seedHex. */
    @Override
    public List<BigDecimal> reveal(String seedHex, int count) {
        byte[] seed = HexFormat.of().parseHex(seedHex);
        return generateWeights(seed, count);
    }

    private byte[] buildSeed(UUID roomId, int roundNumber) {
        // 1. OS entropy — 32 bytes от SecureRandom (/dev/urandom)
        byte[] osEntropy = new byte[32];
        secureRandom.nextBytes(osEntropy);

        // 2. Time entropy — nano + millis = 16 bytes
        byte[] timeEntropy = longToBytes(System.nanoTime(), System.currentTimeMillis());

        // 3. Context entropy — SHA-256("roomId:roundNumber")
        String ctx = roomId.toString() + ":" + roundNumber;
        byte[] ctxEntropy = sha256(ctx.getBytes(StandardCharsets.UTF_8));

        // XOR трёх источников
        byte[] seed = new byte[32];
        for (int i = 0; i < 32; i++) {
            seed[i] = (byte) (osEntropy[i] ^ timeEntropy[i % timeEntropy.length] ^ ctxEntropy[i]);
        }
        return seed;
    }

    private List<BigDecimal> generateWeights(byte[] seed, int count) {
        // Детерминированная генерация: XOR четырёх 8-байтовых чанков — используем все 32 байта.
        long seedLong = readLong(seed, 0) ^ readLong(seed, 8) ^ readLong(seed, 16) ^ readLong(seed, 24);
        Random rng = new Random(seedLong);

        List<BigDecimal> weights = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // Целое число в диапазоне [-10, 10] (21 значение)
            int intWeight = rng.nextInt(WEIGHT_RANGE_SIZE) + WEIGHT_MIN;
            weights.add(BigDecimal.valueOf(intWeight));
        }
        return weights;
    }

    private long readLong(byte[] buf, int offset) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (buf[offset + i] & 0xFFL);
        }
        return v;
    }

    private byte[] longToBytes(long a, long b) {
        byte[] buf = new byte[16];
        for (int i = 7; i >= 0; i--) {
            buf[i] = (byte) (a & 0xFF);
            buf[i + 8] = (byte) (b & 0xFF);
            a >>= 8;
            b >>= 8;
        }
        return buf;
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String sha256Hex(byte[] input) {
        return HexFormat.of().formatHex(sha256(input));
    }
}
