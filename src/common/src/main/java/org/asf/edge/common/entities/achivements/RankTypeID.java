package org.asf.edge.common.entities.achivements;

public enum RankTypeID {

	USER(1),

	DRAGON(8),

	FARMING(9),

	FISHING(10),

	CLAN(11),

	UDT(12);

	private int value;

	private RankTypeID(int value) {
		this.value = value;
	}

	public int getPointTypeID() {
		return value;
	}

	public static RankTypeID getByTypeID(int id) {
		switch (id) {

		case 1:
			return RankTypeID.USER;

		case 8:
			return RankTypeID.DRAGON;

		case 9:
			return RankTypeID.FARMING;

		case 10:
			return RankTypeID.FISHING;

		case 11:
			return RankTypeID.CLAN;

		case 12:
			return RankTypeID.UDT;

		}
		return RankTypeID.USER;
	}

}
