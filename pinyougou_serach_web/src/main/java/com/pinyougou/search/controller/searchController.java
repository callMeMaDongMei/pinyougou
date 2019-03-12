package com.pinyougou.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/itemserach")
public class searchController {
    @Reference(timeout = 20000)
    private ItemSearchService itemSearchService;
    @RequestMapping("/search")
    public Map search(@RequestBody Map searchMap) {
        return itemSearchService.search(searchMap);

    }
}