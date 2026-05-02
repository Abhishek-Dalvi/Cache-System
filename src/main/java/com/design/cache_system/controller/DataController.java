package com.design.cache_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.design.cache_system.services.RedisLuaCacheSystemService;


@Controller
public class DataController {
	
	@Autowired
	private RedisLuaCacheSystemService redisLuaCacheSystemService;

	@GetMapping("/kv")
	public ResponseEntity<String> getMethodName(@RequestParam String param) {
		String ans = null;
		try {
			ans = "For parameter " + param +": " + redisLuaCacheSystemService.getValue(param);
			return ResponseEntity.ok(ans);
		} catch (Exception e) {
			ans =  "Error is: " + e.getMessage();
			return ResponseEntity.badRequest().body(ans);
		}
	}
	
	@PostMapping("/kv")
	public ResponseEntity<String> postData(@RequestParam String key, @RequestParam String value) {
		try {
			redisLuaCacheSystemService.setKV(key, value);
			return ResponseEntity.ok("Data saved successfully");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}
	
}
