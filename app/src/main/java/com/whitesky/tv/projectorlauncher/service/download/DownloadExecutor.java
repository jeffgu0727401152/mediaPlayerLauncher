package com.whitesky.tv.projectorlauncher.service.download;

/**
 * Created by jeff on 18-3-14.
 */

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  线程池工具
 */
public class DownloadExecutor {

    /** 请求线程池队列，同时允许1个线程操作 */
    private ThreadPoolExecutor mPool ;

    //当线程池中的线程小于mCorePoolSize，直接创建新的线程加入线程池执行任务
    private static final int mCorePoolSize = 5;
    //最大线程数
    private static final int mMaximumPoolSize = 5;
    //线程执行完任务后，且队列中没有可以执行的任务，存活的时间，后面的参数是时间单位
    private static final long mKeepAliveTime = 100L;

    private static DownloadExecutor instance;

    public DownloadExecutor() {
        if (mPool == null || mPool.isShutdown()) {
            // 参数说明
            // 当线程池中的线程小于mCorePoolSize，直接创建新的线程加入线程池执行任务
            // 当线程池中的线程数目等于mCorePoolSize，将会把任务放入任务队列BlockingQueue中
            // 当BlockingQueue中的任务放满了，将会创建新的线程去执行，
            // 但是当总线程数大于mMaximumPoolSize时，将会抛出异常，交给RejectedExecutionHandler处理
            // mKeepAliveTime是线程执行完任务后，且队列中没有可以执行的任务，存活的时间，后面的参数是时间单位
            // ThreadFactory是每次创建新的线程工厂
            mPool = new ThreadPoolExecutor(mCorePoolSize, mMaximumPoolSize, mKeepAliveTime,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                    new ThreadFactory() {
                        private AtomicInteger mInteger = new AtomicInteger(1);
                        @Override
                        public Thread newThread(Runnable runnable) {
                            Thread thread = new Thread(runnable, "download thread #" + mInteger.getAndIncrement());
                            return thread;
                        }}, new ThreadPoolExecutor.AbortPolicy());
        }
    }

    public static DownloadExecutor getInstance() {

        if (instance == null) {
            synchronized (DownloadExecutor.class) {
                if (instance == null) {
                    instance = new DownloadExecutor();
                }
            }
        }
        return instance;
    }

    /** 执行任务，当线程池处于关闭，将会重新创建新的线程池 */
    public synchronized void execute(Runnable run) {
        if (run == null) {
            return;
        }
        mPool.execute(run);
    }

    /** 取消线程池中某个还未执行的任务 */
    public synchronized boolean cancel(Runnable run) {
        if (mPool != null && (!mPool.isShutdown() || mPool.isTerminating())) {
            return mPool.getQueue().remove(run);
        }else{
            return false;
        }
    }

    /** 查看线程池中是否还有某个还未执行的任务 */
    public synchronized boolean contains(Runnable run) {
        if (mPool != null && (!mPool.isShutdown() || mPool.isTerminating())) {
            return mPool.getQueue().contains(run);
        } else {
            return false;
        }
    }

    /** 立刻关闭线程池，并且正在执行的任务也将会被中断 */
    public void stop() {
        if (mPool != null && (!mPool.isShutdown() || mPool.isTerminating())) {
            mPool.shutdownNow();
        }
    }

    /** 平缓关闭单任务线程池，但是会确保所有已经加入的任务都将会被执行完毕才关闭 */
    public synchronized void shutdown() {
        if (mPool != null && (!mPool.isShutdown() || mPool.isTerminating())) {
            mPool.shutdown();
        }
    }

    public String toString() {
        return mPool.toString();
    }
}