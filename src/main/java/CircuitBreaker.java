import java.sql.Time;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CircuitBreaker {
    private int retries;
    private int maxRetries;
    private long timeOut;
    private boolean isOpen;
    private ScheduledExecutorService timer;

    public CircuitBreaker() {
        this.retries = 0;
        this.maxRetries = 3;
        this.timeOut = 5;
        this.isOpen = false;
    }

    public CircuitBreaker(int retries, int maxRetries, int seconds) {
        this.retries = retries;
        this.maxRetries = maxRetries;
        this.timeOut = 5;
        this.isOpen = false;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    private void incrementRetry() {
        this.retries++;
    }

    public void updateCircuit(String statusCode) {
        if (statusCode.equals("ERROR")) {
            this.incrementRetry();
        }
    }

    public boolean isCircuitAvailable() {
        if (this.retries <= this.maxRetries) {
            return true;
        } else {
            if (!this.isOpen) {
                this.timer = Executors.newScheduledThreadPool(1);
                this.setOpen(true);
                Runnable task = () -> {
                    this.setOpen(false);
                    this.setRetries(0);
                    this.timer.shutdown();
                };
                this.timer.schedule(task, this.timeOut, TimeUnit.SECONDS);
            }
            return false;
        }
    }


}
