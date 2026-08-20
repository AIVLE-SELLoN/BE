package com.aivle.sellon.rawdb.dto;

public interface ChannelMetricRow {

    String getChannelId();

    interface CsCount extends ChannelMetricRow {

        Long getCount();
    }

    interface OrderQuantity extends ChannelMetricRow {

        Long getTotalQuantity();
    }

    interface ReviewRating extends ChannelMetricRow {

        Double getAvgRating();
    }
}
