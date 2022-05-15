package br.com.homebroker.util;

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

public class EmptyClientCallStreamObserver implements StreamObserver<Empty> {

	@Override
	public void onNext(Empty value) {
		// Nothing to do
	}

	@Override
	public void onError(Throwable t) {
		// Nothing to do
	}

	@Override
	public void onCompleted() {
		// Nothing to do
	}

}
