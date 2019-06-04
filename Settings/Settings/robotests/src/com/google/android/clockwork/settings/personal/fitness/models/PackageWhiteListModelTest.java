package com.google.android.clockwork.settings.personal.fitness.models;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link PackageWhiteListModel}.
 */
@RunWith(ClockworkRobolectricTestRunner.class)
public class PackageWhiteListModelTest {

    private static final String FIT_PACKAGE = "com.google.android.apps.fitness";
    private static final String FIT_COMPONENT =
            "com.google.android.apps.fitness/.realtime.RealtimeActivity";
    private static final String OTHER_PACKAGE = "some.other.package";
    private static final String OTHER_COMPONENT = "some.other.package/.SuperRacerActivity";

    @Test
    public void returnsTrueWhenWhiteListIsEmpty() {
        assertTrue(new PackageWhiteListModel("").isWhiteListed("com.ace.of.race/.RacerActivity"));

        assertTrue(new PackageWhiteListModel("").isWhiteListed(FIT_COMPONENT));
    }

    @Test
    public void returnsTrueWhenPackageWhiteListed() {
        assertTrue(new PackageWhiteListModel(FIT_PACKAGE).isWhiteListed(FIT_PACKAGE));
        assertTrue(new PackageWhiteListModel(FIT_PACKAGE).isWhiteListed(FIT_COMPONENT));

        assertTrue(new PackageWhiteListModel(join(OTHER_PACKAGE, FIT_PACKAGE))
                .isWhiteListed(FIT_COMPONENT));
        assertTrue(new PackageWhiteListModel(join(OTHER_PACKAGE, FIT_PACKAGE))
                .isWhiteListed(FIT_PACKAGE));

        assertTrue(new PackageWhiteListModel(join(OTHER_PACKAGE, FIT_PACKAGE))
                .isWhiteListed(OTHER_COMPONENT));
        assertTrue(new PackageWhiteListModel(join(OTHER_PACKAGE, FIT_PACKAGE))
                .isWhiteListed(OTHER_PACKAGE));
    }

    @Test
    public void returnsFalseWhenPackageNotWhiteListed() {
        assertFalse(new PackageWhiteListModel(join(FIT_PACKAGE, OTHER_PACKAGE))
                .isWhiteListed("not.white.listed"));

        assertFalse(new PackageWhiteListModel(join(FIT_PACKAGE, OTHER_PACKAGE))
                .isWhiteListed("other"));

        assertFalse(new PackageWhiteListModel(",")
                .isWhiteListed(FIT_PACKAGE));
    }

    @Test
    public void returnsFalseWhenPackageSubstringOfWhiteListed() {
        assertFalse(new PackageWhiteListModel(join(FIT_PACKAGE, OTHER_PACKAGE))
                .isWhiteListed("apps.fitness"));
        assertFalse(new PackageWhiteListModel(join(FIT_PACKAGE, OTHER_PACKAGE))
                .isWhiteListed("other"));
    }

    @Test
    public void returnsFalseWhenPackagesHasTrailingCommaAndEmptyStringIsGiven() {
        assertFalse(new PackageWhiteListModel(join(FIT_PACKAGE, OTHER_PACKAGE) + ",")
                .isWhiteListed(""));
        assertFalse(new PackageWhiteListModel(join(FIT_PACKAGE, OTHER_PACKAGE) + ",")
                .isWhiteListed(null));
    }

    @Test
    public void getPackageReturnsPackageWhenGivenPackage() {
        assertEquals(FIT_PACKAGE, PackageWhiteListModel.getPackage(FIT_PACKAGE));
        assertEquals(OTHER_PACKAGE, PackageWhiteListModel.getPackage(OTHER_PACKAGE));
    }

    @Test
    public void getPackageReturnsPackageWhenGivenComponent() {
        assertEquals(FIT_PACKAGE, PackageWhiteListModel.getPackage(FIT_COMPONENT));
        assertEquals(OTHER_PACKAGE, PackageWhiteListModel.getPackage(OTHER_COMPONENT));
    }

    private static String join(String ...strings) {
        return Arrays.stream(strings).collect(Collectors.joining(","));
    }
}