package com.casc.rfidscanner.bean;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Stack {

    private Set<String> mBucketsInStack = new HashSet<>();

    private Set<String> mBucketFound = new HashSet<>();

    private long mCreatedTime;

    public Stack(List<String> buckets) {
        mBucketsInStack.addAll(buckets);
        mCreatedTime = System.currentTimeMillis();
    }

    public List<String> getBuckets() {
        return new LinkedList<>(mBucketsInStack);
    }

    public long getCreatedTime() {
        return mCreatedTime;
    }

    public boolean containBucket(String epcStr) {
        if (mBucketsInStack.contains(epcStr)) {
            mBucketFound.add(epcStr);
        }
        return mBucketsInStack.contains(epcStr);
    }

    public boolean isFound() {
        return mBucketFound.size() >= 3;
    }
}
