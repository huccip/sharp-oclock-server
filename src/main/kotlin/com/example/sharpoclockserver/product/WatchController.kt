package com.example.sharpoclockserver.product

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// GET http://localhost:8080/api/watches
// GET http://localhost:8080/api/watches?id=1234
// GET http://localhost:8080/api/watches/search?q=that_blue_expensive_watch_from_rolex
// POST http://localhost:8080/api/watches/review?id=123,review=4

@RestController
@RequestMapping("/api/watches")
class WatchController(
    val watchService: WatchService
) {
}