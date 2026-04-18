package com.vsrna.game.infrastructure.rng;

import com.vsrna.game.domain.rng.RngPort;
import com.vsrna.game.domain.rng.RngResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final BigDecimal WEIGHT_MIN = new BigDecimal("-50.00");
    private static final BigDecimal WEIGHT_RANGE = new BigDecimal("100.00");
    private static final int SCALE = 2;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public RngResult generate(UUID roomId, int roundNumber, int count) {
        byte[] seed = buildSeed(roomId, roundNumber);
        String seedHex = HexFormat.of().formatHex(seed);
        String seedHash = sha256Hex(seed);

        List<BigDecimal> weights = generateWeights(seed, count);
        return new RngResult(weights, seedHex, seedHash);
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
        // Ранее использовались только первые 8 байт, 75% энтропии терялось.
        long seedLong = readLong(seed, 0) ^ readLong(seed, 8) ^ readLong(seed, 16) ^ readLong(seed, 24);
        Random rng = new Random(seedLong);

        List<BigDecimal> weights = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // [0, 100.00] → сдвиг на -50.00 → [-50.00, 50.00]
            double raw = rng.nextDouble() * 100.0;
            BigDecimal weight = BigDecimal.valueOf(raw)
                    .setScale(SCALE, RoundingMode.HALF_UP)
                    .add(WEIGHT_MIN);
            // Clamp [-50.00, 50.00]
            if (weight.compareTo(WEIGHT_MIN) < 0) weight = WEIGHT_MIN;
            BigDecimal max = WEIGHT_MIN.add(WEIGHT_RANGE);
            if (weight.compareTo(max) > 0) weight = max;
            weights.add(weight);
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
