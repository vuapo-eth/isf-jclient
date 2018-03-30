package isf.logic;

public abstract class CronJob {

    private long nextCallTimeStamp;
    private final long interval;
    private final boolean queueCalls;
    private int openCalls;

    public CronJob(long interval, boolean callImmediately, boolean queueCalls) {
        this.interval = interval;
        this.queueCalls = queueCalls;
        if(callImmediately) call();
        nextCallTimeStamp = System.currentTimeMillis() + interval;
    }

    public abstract void onCall();

    long getNextCallTimeStamp() {
        return nextCallTimeStamp;
    }

    void call() {
        nextCallTimeStamp += interval;
        if(!queueCalls && openCalls > 0) return;

        new Thread() {
            public void run() {
                openCalls++;
                onCall();
                openCalls--;
            };
        }.start();
    }
}