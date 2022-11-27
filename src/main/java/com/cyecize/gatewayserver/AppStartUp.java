package com.cyecize.gatewayserver;

import com.cyecize.ioc.MagicInjector;
import com.cyecize.ioc.annotations.Service;
import com.cyecize.ioc.annotations.StartUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppStartUp {

    public static void main(String[] args) {
        MagicInjector.run(AppStartUp.class);
    }

    @StartUp
    public void startUp() {
        System.out.println("It works!");
    }
}
