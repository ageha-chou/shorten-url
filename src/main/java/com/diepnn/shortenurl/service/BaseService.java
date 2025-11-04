package com.diepnn.shortenurl.service;

public abstract class BaseService {
    protected boolean notBelongToCurrentUser(Long ownerId, Long currentUserId) {
        return ownerId == null || !ownerId.equals(currentUserId);
    }
}
