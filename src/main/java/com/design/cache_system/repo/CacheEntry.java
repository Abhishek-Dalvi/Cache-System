package com.design.cache_system.repo;
/**
 * Having value along with cache expiry time in this object
 */

public class CacheEntry {
	private String value;
	private Long expiryTime;
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Long getExpiryTime() {
		return expiryTime;
	}
	public void setExpiryTime(Long expiryTime) {
		this.expiryTime = expiryTime;
	}
	public CacheEntry(String value, Long expiryTime) {
		this.value = value;
		this.expiryTime = expiryTime;
	}
	
}
