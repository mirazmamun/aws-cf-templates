package de.widdix.awscftemplates;

import com.amazonaws.services.ec2.model.KeyPair;
import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallResults;
import com.evanlennick.retry4j.RetryConfig;
import com.evanlennick.retry4j.RetryConfigBuilder;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.junit.Assert;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

public abstract class ATest {

    protected final <T> T retry(Callable<T> callable) {
        final RetryConfig config = new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(30)
                .withDelayBetweenTries(10, ChronoUnit.SECONDS)
                .withFixedBackoff()
                .build();
        final CallResults<Object> results = new CallExecutor(config).execute(callable);
        return (T) results.getResult();
    }

    protected final void probeSSH(final String host, final KeyPair key) {
        final Callable<Boolean> callable = () -> {
            final JSch jsch = new JSch();
            final Session session = jsch.getSession("ec2-user", host);
            jsch.addIdentity(key.getKeyName(), key.getKeyMaterial().getBytes(), null, null);
            jsch.setConfig("StrictHostKeyChecking", "no"); // for testing this should be fine. adding the host key seems to be only possible via a file which is not very useful here
            session.connect(10000);
            session.disconnect();
            return true;
        };
        Assert.assertTrue(this.retry(callable));
    }

}
