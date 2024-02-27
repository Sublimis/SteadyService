package lib.sublimis.steadyservice;

import android.accessibilityservice.AccessibilityService;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Surface;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

import lib.sublimis.steadyview.HSteadyView;
import lib.sublimis.steadyview.ISteadyView;

public final class SteadyService extends AccessibilityService
{
	public static final double FRESH_COORD_VALUE = 0;

	private static volatile boolean mIsRunning = false;

	private static volatile Thread mActionThread = null;
	private static final Object mActionThreadLock = new Object();
	private volatile boolean mActionThreadShouldQuit = false;

	private static final Object mCoordLock = new Object();
	private static volatile double mCoordX = FRESH_COORD_VALUE, mCoordY = FRESH_COORD_VALUE;
	private volatile double mLastCoordX = FRESH_COORD_VALUE, mLastCoordY = FRESH_COORD_VALUE;

	private final Object mNodesLock = new Object();
	private final List<AccessibilityNodeInfo> mNodes = new ArrayList<>(), mNodesTmp = new ArrayList<>();
	private final Bundle mBundle = new Bundle(), mResetBundle = new Bundle();

	private static volatile ISteadyService mISteadyService = null;

	public interface ISteadyService
	{
		void activate();

		void deactivate();

		int getScreenRotation();

		default boolean putCoords(final double x, final double y, final boolean forced)
		{
			return SteadyService.putCoords(x, y, forced);
		}

		default void onServiceConnected()
		{
		}

		default void onDestroy()
		{
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		startActionThread();
	}

	@Override
	protected void onServiceConnected()
	{
		super.onServiceConnected();

		mIsRunning = true;

		mISteadyService.onServiceConnected();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mIsRunning = false;

		stopActionThread();

		performResetWindowPositions();

		mISteadyService.onDestroy();
	}

	@Override
	public void onAccessibilityEvent(final AccessibilityEvent event)
	{
		/*
		   performResetWindowPositions() doesn't work here, and so was removed.
		   Reason may be that AccessibilityService cannot do actions on nodes which are not in hierarchy anymore; and hierarchy has already changed before this call.
		   We now rely on the client side (SteadyView) to undo steady actions by itself after an idle timeout (2-3 seconds).
		 */
		// performResetWindowPositions();

		if (findMovableNodes(event))
		{
			mISteadyService.activate();
		}
		else
		{
			mISteadyService.deactivate();
		}
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onInterrupt()
	{
	}

	private boolean findMovableNodes(final AccessibilityEvent ignoredEvent)
	{
		boolean retVal = false;

		final List<AccessibilityWindowInfo> windows = getWindows();

		if (windows != null)
		{
			final List<AccessibilityNodeInfo> newNodes = new ArrayList<>();

			{
				final List<AccessibilityNodeInfo> tempList = new ArrayList<>(32);

				for (final AccessibilityWindowInfo window : windows)
				{
					final AccessibilityNodeInfo node = window.getRoot();

					if (node != null)
					{
						tempList.add(node);
					}
				}

				for (int i = 0; i < tempList.size(); i++)
				{
					final AccessibilityNodeInfo node = tempList.get(i);

					if (Build.VERSION.SDK_INT < VERSION_CODES.N || node.isImportantForAccessibility())
					{
						if (node.getActionList()
								.contains(ISteadyView.STEADY_ACTION))
						{
							newNodes.add(node);
						}
					}

					for (int j = 0; j < node.getChildCount(); j++)
					{
						final AccessibilityNodeInfo child = node.getChild(j);

						if (child != null)
						{
							tempList.add(child);
						}
					}
				}
			}

			synchronized (mNodesLock)
			{
				mNodes.clear();
				mNodes.addAll(newNodes);

				retVal = newNodes.size() > 0;
			}
		}

		return retVal;
	}

	public static boolean isRunning()
	{
		return mIsRunning;
	}

	private void actionThreadWork()
	{
		final double x, y;
		final boolean doWork;

		synchronized (mCoordLock)
		{
			x = mCoordX;
			y = mCoordY;

			doWork = x != mLastCoordX || y != mLastCoordY;

			if (doWork)
			{
				mLastCoordX = mCoordX;
				mLastCoordY = mCoordY;
			}
		}

		if (doWork)
		{
			applyCoords(x, y);
		}
	}

	private void startActionThread()
	{
		mActionThreadShouldQuit = false;

		mActionThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				mLastCoordX = mLastCoordY = FRESH_COORD_VALUE;

				while (false == mActionThreadShouldQuit)
				{
					synchronized (mActionThreadLock)
					{
						try
						{
							mActionThreadLock.wait();
						}
						catch (Exception ignored)
						{
						}
					}

					if (false == mActionThreadShouldQuit)
					{
						actionThreadWork();
					}
				}
			}
		}, "SteadyServiceActionThread");

		mActionThread.start();
	}

	private void stopActionThread()
	{
		mActionThreadShouldQuit = true;

		synchronized (mActionThreadLock)
		{
			try
			{
				mActionThreadLock.notify();
			}
			catch (Exception ignored)
			{
			}
		}

		mActionThread = null;
	}

	private static boolean performMoveWindowAction(final List<AccessibilityNodeInfo> nodes, final Bundle args)
	{
		boolean retVal = false;

		final int actionId = ISteadyView.STEADY_ACTION.getId();

		for (final AccessibilityNodeInfo node : nodes)
		{
			retVal |= node.performAction(actionId, args);
		}

		return retVal;
	}

	private boolean performResetWindowPosition(final List<AccessibilityNodeInfo> nodes)
	{
		boolean retVal = false;

		HSteadyView.populateSteadyViewArguments(mResetBundle, 0, 0);

		retVal = performMoveWindowAction(nodes, mResetBundle);

		return retVal;
	}

	private boolean performResetWindowPositions()
	{
		boolean retVal = false;

		final List<AccessibilityNodeInfo> nodes;

		synchronized (mNodesLock)
		{
			nodes = new ArrayList<>(mNodes);
			mNodes.clear();
		}

		retVal = performResetWindowPosition(nodes);

		return retVal;
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private boolean applyCoords(final double x, final double y)
	{
		boolean retVal = true;

		if (isRunning())
		{
			synchronized (mNodesLock)
			{
				mNodesTmp.clear();
				mNodesTmp.addAll(mNodes);
			}

			try
			{
				if (false == mNodesTmp.isEmpty())
				{
					final int screenRotation = mISteadyService.getScreenRotation();

					final double xScr, yScr;

					switch (screenRotation)
					{
						case Surface.ROTATION_0:
						default:
							xScr = x;
							yScr = -y;
							break;
						case Surface.ROTATION_90:
							xScr = y;
							yScr = -x;
							break;
						case Surface.ROTATION_180:
							xScr = -x;
							yScr = y;
							break;
						case Surface.ROTATION_270:
							xScr = -y;
							yScr = x;
							break;
					}

					HSteadyView.populateSteadyViewArguments(mBundle, (int) xScr, (int) yScr);

					retVal = performMoveWindowAction(mNodesTmp, mBundle);
				}
			}
			catch (Exception ignored)
			{
			}
		}

		return retVal;
	}

	private static boolean putCoords(final double x, final double y, final boolean forced)
	{
		final boolean doWork;

		synchronized (mCoordLock)
		{
			doWork = forced || x != mCoordX || y != mCoordY;

			mCoordX = x;
			mCoordY = y;
		}

		if (doWork)
		{
			synchronized (mActionThreadLock)
			{
				try
				{
					mActionThreadLock.notify();
				}
				catch (Exception ignored)
				{
				}
			}
		}

		return doWork;
	}

	public static void setSteadyServiceInterface(final ISteadyService iSteadyService)
	{
		mISteadyService = iSteadyService;
	}
}
