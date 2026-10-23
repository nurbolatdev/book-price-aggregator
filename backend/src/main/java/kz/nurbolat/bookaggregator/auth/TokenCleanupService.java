package kz.nurbolat.bookaggregator.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.function.IntSupplier;

/**
 * Deletes refresh tokens and email links some time after they expire. A revoked refresh token is kept until
 * then: reuse detection needs it, and only after expiry is a copied token useless anyway. Rows go in batches,
 * each in its own transaction, so that a large backlog never locks the tables for long.
 */
@Slf4j
@Service
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokens;
    private final EmailTokenRepository emailTokens;
    private final AuthProperties properties;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public TokenCleanupService(RefreshTokenRepository refreshTokens, EmailTokenRepository emailTokens,
                               AuthProperties properties, PlatformTransactionManager transactionManager, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.emailTokens = emailTokens;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public Result cleanUp() {
        AuthProperties.Cleanup cleanup = properties.cleanup();
        Instant now = clock.instant();
        Instant refreshBefore = now.minus(cleanup.refreshTokensKeep());
        Instant emailBefore = now.minus(cleanup.emailTokensKeep());
        int refresh = deleteInBatches(() -> refreshTokens.deleteExpiredBefore(refreshBefore, cleanup.batchSize()));
        int email = deleteInBatches(() -> emailTokens.deleteExpiredBefore(emailBefore, cleanup.batchSize()));
        return new Result(refresh, email);
    }

    private int deleteInBatches(IntSupplier batch) {
        int batchSize = properties.cleanup().batchSize();
        int total = 0;
        int deleted;
        do {
            Integer count = transaction.execute(status -> batch.getAsInt());
            deleted = count == null ? 0 : count;
            total += deleted;
        } while (deleted == batchSize);
        return total;
    }

    public record Result(int refreshTokens, int emailTokens) {
    }
}
