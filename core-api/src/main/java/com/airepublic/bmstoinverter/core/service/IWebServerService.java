package com.airepublic.bmstoinverter.core.service;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

public interface IWebServerService {

    void start(final int httpPort, final int httpsPort, EnergyStorage energyStorage) throws Exception;


    void stop();
}
