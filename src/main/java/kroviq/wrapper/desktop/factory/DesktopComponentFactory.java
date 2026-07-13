package kroviq.wrapper.desktop.factory;

import kroviq.wrapper.desktop.core.DesktopEngine;
import kroviq.wrapper.desktop.winappdriver.WinAppDriverEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DesktopComponentFactory {

    private static final Logger logger = LogManager.getLogger(DesktopComponentFactory.class);

    public static DesktopEngine createEngine() {
        logger.info("Creating WinAppDriver desktop engine");
        return new WinAppDriverEngine();
    }
}
