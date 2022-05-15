package br.com.exchange.grpc.util;

import br.com.exchange.proto.Identification;

public class Util {

	private Util() {
	}

	private static final String SEPARATOR = "-";

	public static String getKey(Identification identification) {
		return getKey(identification.getTargetCompId(), identification.getSenderCompId());
	}

	public static String getKey(String targetCompId, String senderCompId) {
		StringBuilder sb = new StringBuilder();
		sb.append(targetCompId);
		sb.append(SEPARATOR);
		sb.append(senderCompId);
		return sb.toString();
	}

}
