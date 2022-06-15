package com.example.myapplication;

import com.example.myapplication.slice.MainAbilitySlice;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;

public class MainAbility extends Ability {
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setMainRoute(MainAbilitySlice.class.getName());
        System.out.println("call onStart.....");

    }

    @Override
    protected void onActive() {
        super.onActive();
        System.out.println("call onActive.....");
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        System.out.println("call onInactive.....");

    }

    @Override
    protected void onBackground() {
        super.onBackground();
        System.out.println("call onBackground.....");

    }

    @Override
    protected void onForeground(Intent intent) {
        super.onForeground(intent);
        System.out.println("call onForeground.....");

    }
}
