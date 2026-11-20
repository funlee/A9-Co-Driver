package dev.funlee.a9.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class LegacyPopupChecks {
    static int run(Path assets) throws Exception {
        List<Object[]> samples = List.of(
                new Object[]{LegacyPopup.TIP_ONE, false, new int[]{190,560,0xffffff,540,330,0x0c182c,550,1000,0x0c1a2c}},
                new Object[]{LegacyPopup.TIP_TWO, false, new int[]{564,370,0,580,1132,0xff0054,584,1080,0}},
                new Object[]{LegacyPopup.TIP_THREE, false, new int[]{99,587,0xffffff,152,542,0xfeffff,116,684,0xc3fb14}},
                new Object[]{LegacyPopup.TIP_SEQUENCE, false, new int[]{150,325,0x22685e,496,278,0x45d0bd,580,331,0x030c0c}},
                new Object[]{LegacyPopup.TIP_FIVE, true, new int[]{394,446,0xffffff,88,718,0xffffff,594,200,0}},
                new Object[]{LegacyPopup.AD_BACK, true, new int[]{677,1250,0x020306,677,1238,0x131415,697,1250,0x171818,679,1261,0x131415}},
                new Object[]{LegacyPopup.TIP_FIVE_ALTERNATE, true, new int[]{104,598,0xffffff,102,678,0xc3fb12,216,564,0xffe702,213,710,0xffffff}},
                new Object[]{LegacyPopup.HUD_PROMPT, true, new int[]{505,169,0x0c192a,240,311,0xffffff,233,723,0xffffff,508,1085,0x0c192a}},
                new Object[]{LegacyPopup.ACCOUNT, true, new int[]{477,1068,0xff0054,479,990,0x0c192b,161,474,0x0d0d1d,113,116,0x1497db,41,62,0xffffff}});
        for (Object[] test : samples) {
            LegacyPopup expected = (LegacyPopup)test[0];
            int[] points = (int[])test[2];
            int[] pixels = new int[720*1280];
            java.util.Arrays.fill(pixels, 0x556677);
            for (int i=0; i<points.length; i+=3) pixels[points[i+1]*720+points[i]]=points[i+2];
            if (LegacyPopup.match(new PixelFrame(720,1280,pixels)) != expected ||
                    LegacyPopup.match((x,y)->pixels[y*720+x],true) != ((boolean)test[1]?expected:null))
                throw new AssertionError("Original popup scope: "+expected);
            pixels[points[1]*720+points[0]]=0x556677;
            if (LegacyPopup.match(new PixelFrame(720,1280,pixels)) != null)
                throw new AssertionError("Partial popup signature: "+expected);
        }
        for (String text : List.of("检测到并行存取行为","使用代币购买礼包","需要充值","车辆选择 选择参与此赛事的车辆"))
            if (LegacyPopup.TIP_FIVE.permitsInput(text)) throw new AssertionError("Protected popup: "+text);
        if (!LegacyPopup.TIP_THREE.permitsInput("新公告") || LegacyPopup.ACCOUNT.permitsInput(""))
            throw new AssertionError("Unknown notice and account need distinct outcomes");
        String fixture;
        try (var in=LegacyPopupChecks.class.getResourceAsStream("/legacy-popup.lua")) {
            fixture=new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String mode : List.of("foreground","sequence","background","stale","recovering","modal")) {
            try (LuaRuntime lua=new LuaRuntime((n,s)->0,List.of())) {
                lua.set("mode",mode);
                lua.load("fixture",fixture);
                lua.load("bootstrap", Files.readString(assets.resolve("lua/bootstrap.lua")));
                lua.start("test");
                for (int i=0;i<24;i++) lua.step(i*125L);
                lua.load("verify","assert(done,mode)");
            }
        }
        try (LuaRuntime lua=new LuaRuntime((n,s)->0,List.of())) {
            lua.load("workflow",Files.readString(assets.resolve("lua/workflows.lua")));
            lua.load("completion",String.join("\n",
                    "data={['项目']='特殊赛事',['特赛']=0}; run.data={['次数']='1'}; utils={}",
                    "local reads,confirmed,prepared=0,0,0",
                    "run.list={['特殊赛事']={{'settled',function() return 0 end}}}",
                    "function keepCapture() reads=reads+1 end; function cmpColorEx() return 1 end",
                    "function _sessionLegacyPrepare() prepared=prepared+1 end",
                    "function _sessionLegacyCompleted() confirmed=confirmed+1; return confirmed>1 end",
                    "function test() run.project(); assert(confirmed==2 and reads==2 and prepared==1 and data['特赛']==1); done=true end"));
            lua.start("test");
            for (int i=0;i<30;i++) if (!lua.step(i*250L)) break;
            lua.load("verify","assert(done)");
        }
        return samples.size()*2+12;
    }
}
