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
    private static final int WEIGHT_RANGE_SIZE = 21;

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
        for (int i = 0; i < 32; i++) {
            seed[i] = (byte) (osEntropy[i] ^ timeEntropy[i % timeEntropy.length] ^ ctxEntropy[i]);
        }
        return seed;
    }

    // SHA-256 hash chain in counter mode: each weight is derived from sha256(prev_hash || counter).
    // Preserves all 256 bits of entropy from the seed — no XOR compression to long.
    private List<BigDecimal> generateWeights(byte[] seed, int count) {
        List<BigDecimal> weights = new ArrayList<>(count);
        byte[] current = seed;
        for (int i = 0; i < count; i++) {
            current = sha256(concat(current, intToBytes(i)));
            int raw = readInt(current, 0);
            int intWeight = Math.floorMod(raw, WEIGHT_RANGE_SIZE) + WEIGHT_MIN;
            weights.add(BigDecimal.valueOf(intWeight));
        }
        return weights;
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private byte[] intToBytes(int i) {
        return new byte[]{(byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i};
    }

    private int readInt(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 24) | ((buf[offset + 1] & 0xFF) << 16)
                | ((buf[offset + 2] & 0xFF) << 8) | (buf[offset + 3] & 0xFF);
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
