package org.apache.logging.log4j.core.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConverterKeysSubstitutorTest {

  @Nested
  class LookupConvertKeys {

    @Test
    void date_without_timezone() {

      final Map<String, String> map = new HashMap<>();
      map.put("TIMEZONE", "%d{yyyy-MM-dd HH:mm}");
      final StrLookup lookup = new Interpolator(map);
      final StrSubstitutor subst = new StrSubstitutor(lookup);

      assertEquals(
          "%d{yyyy-MM-dd HH:mm}",
          subst.replace("${env:TIMEZONE:-%d{yyyy-MM-dd HH:mm:ss}}")
      );
      assertEquals(
          "%d{yyyy-MM-dd HH:mm:ss}",
          subst.replace("${env:UNKNOWN:-%d{yyyy-MM-dd HH:mm:ss}}")
      );
    }

    @Test
    void date_with_timezone() {

      final Map<String, String> map = new HashMap<>();
      map.put("TIMEZONE", "%d{yyyy-MM-dd HH:mm}{GMT+07}");
      final StrLookup lookup = new Interpolator(map);
      final StrSubstitutor subst = new StrSubstitutor(lookup);
      assertEquals(
          "%d{yyyy-MM-dd HH:mm}{GMT+07}",
          subst.replace("${env:TIMEZONE:-%d{yyyy-MM-dd HH:mm:ss}{GMT+00}}")
      );
      assertEquals(
          "%d{yyyy-MM-dd HH:mm:ss}{GMT+00}",
          subst.replace("${env:UNKNOWN:-%d{yyyy-MM-dd HH:mm:ss}{GMT+00}}")
      );
    }

    @Test
    void message() {

      final Map<String, String> map = new HashMap<>();
      map.put("MSG", "%m%xEx{filters(${filters})}%n");
      final StrLookup lookup = new Interpolator(map);
      final StrSubstitutor subst = new StrSubstitutor(lookup);
      assertEquals(
          "%m%xEx{filters(${filters})}%n",
          subst.replace("${env:MSG:-%d %highlight{%p} %style{%C{1.} [%t] %m}{bold,green}%n}")
      );
      assertEquals(
          "%d %highlight{%p} %style{%C{1.} [%t] %m}{bold,green}%n",
          subst.replace("${env:UNKNOWN:-%d %highlight{%p} %style{%C{1.} [%t] %m}{bold,green}%n}")
      );
    }

    @Test
    void max_length() {

      final Map<String, String> map = new HashMap<>();
      map.put("MSG", "%maxLen{%p: %C{1.} - %m%notEmpty{ =>%ex{short}}}{160}");
      final StrLookup lookup = new Interpolator(map);
      final StrSubstitutor subst = new StrSubstitutor(lookup);
      assertEquals(
          "%maxLen{%p: %C{1.} - %m%notEmpty{ =>%ex{short}}}{160}",
          subst.replace("${env:MSG:-%maxLen{%p: %c{1} - %m%notEmpty{ =>%ex{short}}}{150}}")
      );
      assertEquals(
          "%maxLen{%p: %c{1} - %m%notEmpty{ =>%ex{short}}}{150}",
          subst.replace("${env:UNKNOWN:-%maxLen{%p: %c{1} - %m%notEmpty{ =>%ex{short}}}{150}}")
      );
    }

  }

  @Test
  void test_missing_braces() {

    final Map<String, String> map = new HashMap<>();
    map.put("MSG", "%maxLen{%p: %C{1.} - %m%notEmpty{ =>%ex{short}}}{160}");
    final StrLookup lookup = new Interpolator(map);
    final StrSubstitutor subst = new StrSubstitutor(lookup);
    assertEquals(
        "% %p{{}}}",
        subst.replace("${env:UNKNOWN:-% %p{{}}}}")
    );
  }

}