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
import java.util.UUID;

@Service
public class RngServiceImpl implements RngPort {

    private static final int WEIGHT_MIN = -10;
    private static final int WEIGHT_MAX = 10;
    private static final int WEIGHT_RANGE_SIZE = WEIGHT_MAX - WEIGHT_MIN + 1;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public RngCommitment commit(UUID roomId, int roundNumber) {
        byte[] seed = buildSeed(roomId, roundNumber);
        String seedHex = HexFormat.of().formatHex(seed);
        String seedHash = sha256Hex(seed);
        return new RngCommitment(seedHex, seedHash);
    }

    @Override
    public List<BigDecimal> reveal(String seedHex, int count) {
        byte[] seed = HexFormat.of().parseHex(seedHex);
        return generateWeights(seed, count);
    }

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

    // SHA-256 hash chain in counter mode: each weight is derived from sha256(prev_hash || counter).
    // Preserves all 256 bits of entropy from the seed — no XOR compression to long.
    private List<BigDecimal> generateWeights(byte[] seed, int count) {
        List<BigDecimal> weights = new ArrayList<>(count);
        byte[] current = seed;
        for (int weightIndex = 0; weightIndex < count; weightIndex++) {
            current = sha256(concat(current, intToBytes(weightIndex)));
            int raw = readInt(current, 0);
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
