package com.github.talrey.createdeco;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.CreativeModeTab;

public class CreativeTabs {
  public static final String PROPS_KEY = "props_tab";
  public static final String BRICKS_KEY = "bricks_tab";
  @ExpectPlatform
  public static CreativeModeTab props () {
    throw new AssertionError();
  }

  public static CreativeModeTab bricks () { throw new AssertionError(); }
}
