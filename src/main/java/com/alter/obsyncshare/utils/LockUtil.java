package com.alter.obsyncshare.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockUtil {
	private static final Logger logger = LoggerFactory.getLogger(LockUtil.class);

	static class WeakReferenceWrapper extends WeakReference<ReentrantLock> {
		private String key;

		public WeakReferenceWrapper(String key, ReentrantLock referent, ReferenceQueue<? super ReentrantLock> q) {
			super(referent, q);
			this.key = key;
		}

		public String getKey() {
			return key;
		}

	}

	static {
		Thread thread = new Thread(() -> {
			clean();
		});
		thread.setDaemon(true);
		thread.start();
		logger.debug("lockUtil 守护线程开启");
	}
	// 多线程读取hashmap可以 写入则需要加锁 update ->保险起见改用ConcurrentHashMap
	// ，不确定hashmap多线程读一线程写是否有问题
	final private static Map<String, WeakReferenceWrapper> LOCK_MAP = new ConcurrentHashMap<>();

	final private static ReferenceQueue<? super ReentrantLock> queue = new ReferenceQueue<>();

	final private static Lock changeLock = new ReentrantLock();

	/**
	 * 
	 * 注意gc时会将弱引用回收掉，return之前保证强引用到锁对象上 以防止返回一个null
	 */
	public static Lock lock(String lockName) {
		// clean();
		// 此时可能发生了gc
		WeakReferenceWrapper lockWeak = LOCK_MAP.get(lockName);
		// 发生gc后可能拿到的是null
		if (lockWeak == null) {
			return createLock(lockName);
		}
		// 强引用上 防止此时gc设置为空了
		ReentrantLock lock = lockWeak.get();
		if (lock == null) {
			return createLock(lockName);
		}
		return lock;
	}

	private static void clean() {
		// remove 和 put 必须加锁 hashmap线程不安全
		try {
			WeakReferenceWrapper k;
			while ((k = (WeakReferenceWrapper) queue.remove()) != null) {
				// 用了ConcurrentHashMap 这里可以考虑不用加锁了
				changeLock.lock();
				try {
					LOCK_MAP.remove(k.getKey());
				} finally {
					changeLock.unlock();
				}
				logger.debug(k.getKey() + " lock已被回收");
			}
		} catch (InterruptedException e) {
			// 结束循环
		}
	}

	private static ReentrantLock createLock(String lockName) {
		// remove 和 put 必须加锁 hashmap线程不安全
		changeLock.lock();
		try {
			// 如果前面已经有人创建了这个名字的锁则直接返回
			WeakReferenceWrapper lockWeak = LOCK_MAP.get(lockName);
			if (lockWeak != null) {
				ReentrantLock lock = lockWeak.get();
				if (lock != null) {
					// weak 和 lock 都存在时则返回 ，如果不存在说明上一个线程创建完锁后 触发了GC 导致刚准备拿锁时被回收了
					return lock;
				}
			}
			// 走到这里说明MAP里面的锁已经被回收了 需要创建锁
			// lock对象始终保证强引用 以防止被回收
			ReentrantLock lock = new ReentrantLock();
			WeakReferenceWrapper weakReference = new WeakReferenceWrapper(lockName, lock, queue);
			LOCK_MAP.put(lockName, weakReference);
			return lock;
		} finally {
			changeLock.unlock();
		}
	}
}
