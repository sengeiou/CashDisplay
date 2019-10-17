package com.resonance.cashdisplay.http;

public class ShellOutput {

    public final int id;
    public final String output;

    public ShellOutput(final int id, final String output) {
        this.id = id;
        this.output = output;
    }

    public interface OnShellOutputListener {
        void onShellOutput(final ShellOutput shellOutput);
    }

}