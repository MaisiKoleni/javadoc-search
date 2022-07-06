package net.maisikoleni.javadoc.service.jdk18;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@QuarkusTest
class Jdk18SearchSerivceTest {

	@Inject
	@Jdk(Version.RELEASE_18)
	SearchService searchService;

	@Test
	void testBaseUrl() {
		assertThat(searchService.javadoc().baseUrl())
				.isEqualTo(URI.create("https://docs.oracle.com/en/java/javase/18/docs/api/"));
	}

	@Test
	void testSearchSet() {
		var results = searchAsList("Set");
		assertThat(results).startsWith(linesToArray("""
				java.base/java.util.Set
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(int[][])
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(int, int)
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(String)
				java.desktop/javax.print.attribute.SetOfIntegerSyntax
				jdk.jfr/jdk.jfr.SettingControl
				java.desktop/javax.print.attribute.SetOfIntegerSyntax.SetOfIntegerSyntax(int)
				jdk.jfr/jdk.jfr.SettingControl.SettingControl()
				jdk.jfr/jdk.jfr.SettingDefinition
				jdk.jfr/jdk.jfr.SettingDescriptor
				jdk.management.jfr/jdk.management.jfr.SettingDescriptorInfo
				java.base/java.lang.constant.ConstantDescs.CD_Set
				java.base/java.util.AbstractSet
				java.base/java.util.BitSet
				java.base/java.util.Calendar.areFieldsSet
				java.base/java.util.Calendar.isSet
				java.base/java.util.Calendar.isTimeSet
				java.base/java.util.EnumSet
				java.base/java.util.HashSet
				java.base/java.util.LinkedHashSet
				java.base/java.util.NavigableSet
				java.base/java.util.SortedSet
				java.base/java.util.TreeSet
				java.base/java.util.concurrent.ConcurrentSkipListSet
				java.base/java.util.concurrent.CopyOnWriteArraySet
				java.desktop/javax.accessibility.AccessibleRelationSet
				java.desktop/javax.accessibility.AccessibleStateSet
				java.desktop/javax.imageio.ImageWriteParam.tilingSet
				java.desktop/javax.imageio.plugins.tiff.BaselineTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifGPSTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifInteroperabilityTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifParentTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.ExifTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.FaxTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.GeoTIFFTagSet
				java.desktop/javax.imageio.plugins.tiff.TIFFTagSet
				java.desktop/javax.print.attribute.AttributeSet
				java.desktop/javax.print.attribute.DocAttributeSet
				java.desktop/javax.print.attribute.HashAttributeSet
				java.desktop/javax.print.attribute.HashDocAttributeSet
				java.desktop/javax.print.attribute.HashPrintJobAttributeSet
				java.desktop/javax.print.attribute.HashPrintRequestAttributeSet
				java.desktop/javax.print.attribute.HashPrintServiceAttributeSet
				java.desktop/javax.print.attribute.PrintJobAttributeSet
				java.desktop/javax.print.attribute.PrintRequestAttributeSet
				java.desktop/javax.print.attribute.PrintServiceAttributeSet
				java.desktop/javax.swing.JViewport.isViewSizeSet
				java.desktop/javax.swing.text.AttributeSet
				java.desktop/javax.swing.text.MutableAttributeSet
				java.desktop/javax.swing.text.SimpleAttributeSet
				java.desktop/javax.swing.text.StyleConstants.TabSet
				java.desktop/javax.swing.text.StyleContext.SmallAttributeSet
				java.desktop/javax.swing.text.TabSet
				java.sql.rowset/javax.sql.rowset.BaseRowSet
				java.sql.rowset/javax.sql.rowset.CachedRowSet
				java.sql.rowset/javax.sql.rowset.FilteredRowSet
				java.sql.rowset/javax.sql.rowset.JdbcRowSet
				java.sql.rowset/javax.sql.rowset.JoinRowSet
				java.sql.rowset/javax.sql.rowset.WebRowSet
				java.sql/java.sql.ResultSet
				java.sql/javax.sql.RowSet
				jdk.jdi/com.sun.jdi.event.EventSet
				""")).hasSize(760);
	}

	@Test
	void testSearchComparableCompareTo() {
		var results = searchAsList("java.base/java.lang.Comparable.compareTo(T)");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.lang.Comparable.compareTo(T)
				"""));
	}

	@Test
	void testSearchDotCollector() {
		var results = searchAsList(".Collector");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.util.stream.Collector
				java.base/java.util.stream.Collectors
				"""));
	}

	@Test
	void testSearchLowersMathSpaceMax() {
		var results = searchAsList("math max");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.lang.Math.max(int, int)
				java.base/java.lang.Math.max(float, float)
				java.base/java.lang.Math.max(double, double)
				java.base/java.lang.Math.max(long, long)
				java.base/java.lang.StrictMath.max(double, double)
				java.base/java.lang.StrictMath.max(long, long)
				java.base/java.lang.StrictMath.max(float, float)
				java.base/java.lang.StrictMath.max(int, int)
				"""));
	}

	@Test
	void testSearchLowersAbstractSetTildes() {
		var results = searchAsList("a~s~set");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.util.AbstractSet
				java.base/java.util.AbstractSet.AbstractSet()
				"""));
	}

	@Test
	void testSearchLowersDotAttributeSetTildes() {
		var results = searchAsList(".a~set");
		assertThat(results).containsExactly(linesToArray("""
				java.desktop/javax.print.attribute.AttributeSet
				java.desktop/javax.swing.text.AttributeSet
				java.desktop/javax.print.attribute.AttributeSetUtilities
				"""));
	}

	@Test
	void testSearchIOOBE() {
		var results = searchAsList("IOOBE");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.lang.IndexOutOfBoundsException
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException(long)
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException(int)
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException(String)
				java.base/java.lang.IndexOutOfBoundsException.IndexOutOfBoundsException()
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException(String)
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException()
				java.base/java.lang.StringIndexOutOfBoundsException.StringIndexOutOfBoundsException(String)
				java.base/java.lang.StringIndexOutOfBoundsException.StringIndexOutOfBoundsException()
				java.base/java.lang.ArrayIndexOutOfBoundsException.ArrayIndexOutOfBoundsException(int)
				java.base/java.lang.ArrayIndexOutOfBoundsException
				java.base/java.lang.StringIndexOutOfBoundsException.StringIndexOutOfBoundsException(int)
				java.base/java.lang.StringIndexOutOfBoundsException
				"""));
	}

	@Test
	void testSearchAwaitNoArgs() {
		var results = searchAsList("aw()");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.util.concurrent.CountDownLatch.await()
				java.base/java.util.concurrent.CyclicBarrier.await()
				java.base/java.util.concurrent.locks.AbstractQueuedLongSynchronizer.ConditionObject.await()
				java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject.await()
				java.base/java.util.concurrent.locks.Condition.await()
				"""));
	}

	@Test
	void testSearchFilesWithModule() {
		var results = searchAsList("java.base/...Files");
		assertThat(results).containsExactly(linesToArray("""
				java.base/java.nio.file.Files
				"""));
	}

	List<CharSequence> searchAsList(String query) {
		return searchService.searchEngine().search(query).map(SearchableEntity::qualifiedName).toList();
	}

	static String[] linesToArray(String text) {
		return text.lines().toArray(String[]::new);
	}
}
