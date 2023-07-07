package com.lukk.sky.offer.domain.ports.service;

import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;

public interface EventSourceService {
    void saveEvent(Offer savedOffer, EventType eventType);
}
