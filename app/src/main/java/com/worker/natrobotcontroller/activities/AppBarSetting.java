package com.worker.natrobotcontroller.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import kotlin.jvm.internal.Intrinsics;

public class AppBarSetting extends PreferenceActivity {
    private AppCompatDelegate mDelegate;
    private HashMap _$_findViewCache;

    @Nullable
    public final ActionBar getSupportActionBar() {
        return this.getDelegate().getSupportActionBar();
    }

    private final AppCompatDelegate getDelegate() {
        if(this.mDelegate == null) {
            this.mDelegate = AppCompatDelegate.create((Activity)this, (AppCompatCallback)null);
        }

        AppCompatDelegate var10000 = this.mDelegate;
        if(this.mDelegate == null) {
            Intrinsics.throwNpe();
        }

        return var10000;
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.getDelegate().installViewFactory();
        this.getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        ActionBar var10000 = this.getSupportActionBar();
        if(var10000 != null) {
            var10000.setDisplayHomeAsUpEnabled(true);
        }

    }

    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        Intrinsics.checkParameterIsNotNull(item, "item");
        switch(item.getItemId()) {
            case 16908332:
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.getDelegate().onPostCreate(savedInstanceState);
    }

    public final void setSupportActionBar(@NotNull Toolbar toolbar) {
        Intrinsics.checkParameterIsNotNull(toolbar, "toolbar");
        this.getDelegate().setSupportActionBar(toolbar);
    }

    @NotNull
    public MenuInflater getMenuInflater() {
        MenuInflater var10000 = this.getDelegate().getMenuInflater();
        Intrinsics.checkExpressionValueIsNotNull(var10000, "delegate.menuInflater");
        return var10000;
    }

    public void setContentView(@LayoutRes int layoutResID) {
        this.getDelegate().setContentView(layoutResID);
    }

    public void setContentView(@NotNull View view) {
        Intrinsics.checkParameterIsNotNull(view, "view");
        this.getDelegate().setContentView(view);
    }

    public void setContentView(@NotNull View view, @NotNull LayoutParams params) {
        Intrinsics.checkParameterIsNotNull(view, "view");
        Intrinsics.checkParameterIsNotNull(params, "params");
        this.getDelegate().setContentView(view, params);
    }

    public void addContentView(@NotNull View view, @NotNull LayoutParams params) {
        Intrinsics.checkParameterIsNotNull(view, "view");
        Intrinsics.checkParameterIsNotNull(params, "params");
        this.getDelegate().addContentView(view, params);
    }

    protected void onPostResume() {
        super.onPostResume();
        this.getDelegate().onPostResume();
    }

    protected void onTitleChanged(@NotNull CharSequence title, int color) {
        Intrinsics.checkParameterIsNotNull(title, "title");
        super.onTitleChanged(title, color);
        this.getDelegate().setTitle(title);
    }

    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        Intrinsics.checkParameterIsNotNull(newConfig, "newConfig");
        super.onConfigurationChanged(newConfig);
        this.getDelegate().onConfigurationChanged(newConfig);
    }

    protected void onStop() {
        super.onStop();
        this.getDelegate().onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        this.getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        this.getDelegate().invalidateOptionsMenu();
    }

}
