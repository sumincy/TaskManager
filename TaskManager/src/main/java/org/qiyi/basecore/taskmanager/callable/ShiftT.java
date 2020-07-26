package org.qiyi.basecore.taskmanager.callable;

import androidx.annotation.CallSuper;

import org.qiyi.basecore.taskmanager.Task;
import org.qiyi.basecore.taskmanager.callable.iface.CallEachT;
import org.qiyi.basecore.taskmanager.callable.iface.IAfterCall;
import org.qiyi.basecore.taskmanager.callable.iface.IPreCall;
import org.qiyi.basecore.taskmanager.callable.iface.ShiftCallT;

import java.util.LinkedList;

public abstract class ShiftT<T> extends Shift<T> {
    protected CallEachT<T> mEach;
    protected ShiftT<T> mParent;
    private LinkedList<ShiftT<T>> mChildren = new LinkedList<>();



    public final void call(CallEachT<T> value) {
        mEach = value;
        run();
    }

    public final void callAsync(CallEachT<T> value) {
        mEach = value;

        new Task() {

            @Override
            public void doTask() {
                run();
            }
        }.postAsync();
    }


    /**
     * T:  传入的参数类型
     * R:  返回的参数类型
     *
     * @param each
     * @param <R>
     * @return
     */
    public final <R> ShiftT<R> shiftT(ShiftCallT<T, ShiftT<R>> each) {

        IterableEachCall<R> result = new IterableEachCall<>();
        result.setCall(this);
        if (mChildren.isEmpty()) {
            // each should return this type;
            shiftEach(result, each);

        } else {
            for (ShiftT<T> t : mChildren) {
                t.preCall(mPreCall);
                t.afterCall(mAfterCall);
                result.addNext(t.shiftT(each));
            }
        }
        // set after build
        return result;
    }


    public final <K, V> MapEachCall<K, V> shiftKV(ShiftCallT<T, ? extends MapEachCall<K, V>> each) {

        MapEachCall<K, V> result = new MapEachCall<>();
        result.setCall(this);

        if (mChildren.isEmpty()) {
            // each should return this type;
            shiftEach(result, each);
        } else {
            for (ShiftT<T> t : mChildren) {
                t.preCall(mPreCall);
                t.afterCall(mAfterCall);
                result.addNext(t.shiftKV(each));
            }
        }
        return result;
    }

    protected void addNext(ShiftT<T> value) {
        if (value != null) {
            mChildren.addLast(value);
            value.mParent = this;
        }
    }

    private void run() {
        if(mPreCall != null || mAfterCall != null) {
            // prepare build call
            doCallEach(null);
            mPreCall = null;
            mAfterCall = null;


        }
        if(mEach != null) {
            doPreCall();
            doCallEach(mEach);
            doAfterCall();
        }
    }

    private void doCallEach(CallEachT<T> each){
        if (mChildren.isEmpty()) {
            callEach(each);
        } else {
            for (ShiftT<T> var : mChildren) {
                var.preCall(mPreCall);
                var.afterCall(mAfterCall);
                var.call(each);
            }
        }
    }

    public ShiftT<T> preCall(IPreCall<T> preCall) {

        mPreCall = preCall;

        return this;
    }

    public ShiftT<T> afterCall(IAfterCall<T> afterCall) {
        mAfterCall = afterCall;
        return this;
    }

    @Override
    void addPreCall(PreCall<?> preCall) {
        if(mParent != null) {
            mParent.addPreCall(preCall);
        } else {
            super.addPreCall(preCall);
        }
    }

    protected abstract <R> void shiftEach(ShiftT<R> chain, ShiftCallT<T, ? extends ShiftT<R>> each);

    protected abstract <K, V> void shiftEach(ShiftKV<K, V> chain, ShiftCallT<T, ? extends ShiftKV<K, V>> each);

    protected abstract void callEach(CallEachT<T> call);

}
