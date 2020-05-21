package com.rubbertranslator.modules.textinput.clipboard;


import com.rubbertranslator.event.ClipboardContentInputEvent;
import com.rubbertranslator.event.CopyOriginOrTranslationEvent;
import com.rubbertranslator.event.TranslatorProcessEvent;
import com.rubbertranslator.modules.filter.ProcessFilter;
import com.rubbertranslator.system.SystemResourceManager;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;


public class ClipboardListenerThread extends Thread {
    // 系统剪切板
    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    // 程序终止
    private volatile boolean exit = false;
    // 程序暂停
    private volatile boolean running = true;
    private final Object blocker = new Object();

    // 过滤器
    private ProcessFilter processFilter;

    public ProcessFilter getProcessFilter() {
        return processFilter;
    }

    // 跳过本次监听变化
    private boolean ignoreThisTime = false;

    public void setProcessFilter(ProcessFilter processFilter) {
        this.processFilter = processFilter;
    }

    public ClipboardListenerThread() {
        setName("Clipboard Thread");
    }

    private void init(){
        EventBus.getDefault().register(this);
    }

    private void destroy(){
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void run() {
        init();
        final long minWaitTime = 100;
        final long maxWaitTime = 3000;
        // 动态等待时间
        long waitTime = minWaitTime;
        String initialText = "";
        Image initialImage = new BufferedImage(1, 1, TYPE_INT_RGB);
        // 通知事件Event
        ClipboardContentInputEvent textInputEvent = new ClipboardContentInputEvent();
        // 剪切板内容对象
        Transferable t;
        while (!exit) {
            try {
                // xxx:怎么避免浪费CPU时间？
                Thread.sleep(waitTime);
                if (!running){  // 暂停
                    synchronized (blocker){
                        blocker.wait();
                    }
                }

                t = clipboard.getContents(null);
                // XXX: 下面的代码判断重复多余，但是尚未找到好的方法来区别不同的Transferable
                if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String paste = (String) t.getTransferData(DataFlavor.stringFlavor);
                    if (!Objects.equals(paste, initialText)) {
                        initialText = paste;
                        // 本次忽略
                        if(ignoreThisTime){
                            ignoreThisTime = false;
                        }else{
                            // 正常流程
                            // 过滤器
                            if(processFilter!=null && !processFilter.check()){
                                textInputEvent.setText(paste);
                                EventBus.getDefault().post(textInputEvent);
                            }
                        }
                    }
                } else if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    Image paste = (Image) t.getTransferData(DataFlavor.imageFlavor);
                    if ((paste.getWidth(null) != initialImage.getWidth(null)
                            || paste.getHeight(null) != initialImage.getHeight(null))) {
                        initialImage = paste;

                        if(ignoreThisTime){
                            ignoreThisTime = false;
                        }else{
                            if(processFilter!=null && !processFilter.check()){
                                textInputEvent.setImage(paste);
                                EventBus.getDefault().post(textInputEvent);
                            }
                        }
                    }
                }

            } catch (InterruptedException | IllegalStateException | UnsupportedFlavorException | IOException e) {
                Logger.getLogger(ClipboardListenerThread.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                if (waitTime < maxWaitTime) {
                    waitTime += 100;  // 增加100ms等待时间
                }
            } finally {
                if (waitTime > minWaitTime) {
                    waitTime -= 10;   // 减少10ms等待时间
                }
            }
        }
        destroy();
        Logger.getLogger(this.getClass().getName()).info("ClipBoard exit");
    }

    public void setRun(boolean run){
        if(run){
            resumeRun();
        }else{
            pause();
        }
    }

    private void pause() {
        running = false;
    }

    private void resumeRun() {
        running = true;
        synchronized (blocker){
            blocker.notify();
        }
    }

    public void exit() {
        exit = true;
    }

    /**
     * translatorProcessEvent事件接收 翻译过程开始或结束
     * @param event translatorEvent事件接收
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void translatorProcessEvent(TranslatorProcessEvent event) {
        if (event == null) return;
        if(event.isProcessStart()){
            pause();    // 事件处理开始，暂停接收新变化
        }else{
            if(SystemResourceManager.getConfigurationProxy().getAfterProcessorConfig().isAutoCopy()){
                // 如果当前系统开启自动复制，必须忽略本次剪切板变化，避免重复翻译
                ignoreThisTime=true;
            }
            resumeRun();
        }
    }

    /**
     * 用户复制原文或译文时，为了避免重复翻译，监听线程需要忽略本次剪切板变化
     * @param event 复制原文或译文事件
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void triggerIgnoreThisTime(CopyOriginOrTranslationEvent event) {
        if(event == null) return;
        ignoreThisTime = true;
    }
}