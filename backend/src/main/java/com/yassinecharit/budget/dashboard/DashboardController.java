package com.yassinecharit.budget.dashboard;

import com.yassinecharit.budget.common.MonthParam;
import com.yassinecharit.budget.dashboard.DashboardDtos.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse get(@RequestParam(required = false) String month) {
        return service.build(MonthParam.parse(month));
    }
}
