package com.nhjclxc.utils;

import org.springframework.lang.Nullable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor {

    public static final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            5,
            10,
            30,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5),
            new CustomThreadFactory(),
            new CustomRejectedExecutionHandler());


    /**
     * 线程池初始化方法
     * <p>
     * 1.当线程池小于corePoolSize时，新提交任务将创建一个新线程执行任务，即使此时线程池中存在空闲线程。
     * 2.当线程池达到corePoolSize时，新提交任务将被放入workQueue中，等待线程池中任务调度执行
     * 3.当workQueue已满，且maximumPoolSize>corePoolSize时，新提交任务会创建新线程执行任务
     * 4.当提交任务数超过maximumPoolSize时，新提交任务由RejectedExecutionHandler处理
     * 5.当线程池中超过corePoolSize线程，空闲时间达到keepAliveTime时，关闭空闲线程
     * 6.当设置allowCoreThreadTimeOut(true)时，线程池中corePoolSize线程空闲时间达到keepAliveTime也将关闭
     * <p>
     * corePoolSize 核心线程池大小----5
     * maximumPoolSize 最大线程池大小----10
     * keepAliveTime 线程池中超过corePoolSize数目的空闲线程最大存活时间----30+单位TimeUnit
     * TimeUnit keepAliveTime时间单位----TimeUnit.MINUTES
     * workQueue 阻塞队列----new ArrayBlockingQueue<Runnable>(5)====5容量的阻塞队列
     * threadFactory 新建线程工厂----new CustomThreadFactory()====定制的线程工厂
     * rejectedExecutionHandler 当提交任务数超过maxmumPoolSize+workQueue之和时,
     * 即当提交第41个任务时(前面线程都没有执行完,此测试方法中用sleep(100)),
     * 任务会交给RejectedExecutionHandler来处理
     */


    public static void destory() {
        pool.shutdownNow();
        System.out.println("线程池关闭");
    }


    private static class CustomThreadFactory implements ThreadFactory {

        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(@Nullable Runnable r) {
            Thread t = new Thread(r);
            String threadName = CustomThreadPoolExecutor.class.getSimpleName() + count.addAndGet(1);
            t.setName(threadName);
            return t;
        }
    }


    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
