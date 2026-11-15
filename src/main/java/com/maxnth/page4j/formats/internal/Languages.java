package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.model.value.Language;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import org.jspecify.annotations.Nullable;

/**
 * Maps ISO 639 language codes to PAGE languages and back. Two-letter codes (639-1), three-letter
 * terminology and bibliographic codes (639-2/T, 639-2/B) are understood, with a region or other
 * subtag ({@code en-GB}, {@code deu_frak}) ignored.
 */
public final class Languages {

  /** ISO 639-1 code and PAGE language name, in the order the writer prefers. */
  private static final String[] CODES = {
    "ab", "Abkhaz",
    "aa", "Afar",
    "af", "Afrikaans",
    "ak", "Akan",
    "sq", "Albanian",
    "am", "Amharic",
    "ar", "Arabic",
    "an", "Aragonese",
    "hy", "Armenian",
    "as", "Assamese",
    "av", "Avaric",
    "ae", "Avestan",
    "ay", "Aymara",
    "az", "Azerbaijani",
    "bm", "Bambara",
    "ba", "Bashkir",
    "eu", "Basque",
    "be", "Belarusian",
    "bn", "Bengali",
    "bh", "Bihari",
    "bi", "Bislama",
    "bs", "Bosnian",
    "br", "Breton",
    "bg", "Bulgarian",
    "my", "Burmese",
    "km", "Cambodian",
    "ca", "Catalan",
    "ch", "Chamorro",
    "ce", "Chechen",
    "ny", "Chichewa",
    "zh", "Chinese",
    "cv", "Chuvash",
    "kw", "Cornish",
    "co", "Corsican",
    "cr", "Cree",
    "hr", "Croatian",
    "cs", "Czech",
    "da", "Danish",
    "dv", "Divehi",
    "nl", "Dutch",
    "dz", "Dzongkha",
    "en", "English",
    "eo", "Esperanto",
    "et", "Estonian",
    "ee", "Ewe",
    "fo", "Faroese",
    "fj", "Fijian",
    "fi", "Finnish",
    "fr", "French",
    "ff", "Fula",
    "gd", "Gaelic",
    "gl", "Galician",
    "lg", "Ganda",
    "ka", "Georgian",
    "de", "German",
    "el", "Greek",
    "gn", "Guaraní",
    "gu", "Gujarati",
    "ht", "Haitian",
    "ha", "Hausa",
    "he", "Hebrew",
    "hz", "Herero",
    "hi", "Hindi",
    "ho", "Hiri Motu",
    "hu", "Hungarian",
    "is", "Icelandic",
    "io", "Ido",
    "ig", "Igbo",
    "id", "Indonesian",
    "ia", "Interlingua",
    "ie", "Interlingue",
    "iu", "Inuktitut",
    "ik", "Inupiaq",
    "ga", "Irish",
    "it", "Italian",
    "ja", "Japanese",
    "jv", "Javanese",
    "kl", "Kalaallisut",
    "kn", "Kannada",
    "kr", "Kanuri",
    "ks", "Kashmiri",
    "kk", "Kazakh",
    "ki", "Kikuyu",
    "rw", "Kinyarwanda",
    "rn", "Kirundi",
    "kv", "Komi",
    "kg", "Kongo",
    "ko", "Korean",
    "ku", "Kurdish",
    "kj", "Kwanyama",
    "ky", "Kyrgyz",
    "lo", "Lao",
    "la", "Latin",
    "lv", "Latvian",
    "li", "Limburgish",
    "ln", "Lingala",
    "lt", "Lithuanian",
    "lu", "Luba-Katanga",
    "lb", "Luxembourgish",
    "mk", "Macedonian",
    "mg", "Malagasy",
    "ms", "Malay",
    "ml", "Malayalam",
    "mt", "Maltese",
    "gv", "Manx",
    "mi", "Māori",
    "mr", "Marathi",
    "mh", "Marshallese",
    "mn", "Mongolian",
    "na", "Nauru",
    "nv", "Navajo",
    "ng", "Ndonga",
    "ne", "Nepali",
    "nd", "North Ndebele",
    "se", "Northern Sami",
    "no", "Norwegian",
    "nb", "Norwegian Bokmål",
    "nn", "Norwegian Nynorsk",
    "ii", "Nuosu",
    "oc", "Occitan",
    "oj", "Ojibwe",
    "cu", "Old Church Slavonic",
    "or", "Oriya",
    "om", "Oromo",
    "os", "Ossetian",
    "pi", "Pāli",
    "pa", "Panjabi",
    "ps", "Pashto",
    "fa", "Persian",
    "pl", "Polish",
    "pt", "Portuguese",
    "qu", "Quechua",
    "ro", "Romanian",
    "rm", "Romansh",
    "ru", "Russian",
    "sm", "Samoan",
    "sg", "Sango",
    "sa", "Sanskrit",
    "sc", "Sardinian",
    "sr", "Serbian",
    "sn", "Shona",
    "sd", "Sindhi",
    "si", "Sinhala",
    "sk", "Slovak",
    "sl", "Slovene",
    "so", "Somali",
    "nr", "South Ndebele",
    "st", "Southern Sotho",
    "es", "Spanish",
    "su", "Sundanese",
    "sw", "Swahili",
    "ss", "Swati",
    "sv", "Swedish",
    "tl", "Tagalog",
    "ty", "Tahitian",
    "tg", "Tajik",
    "ta", "Tamil",
    "tt", "Tatar",
    "te", "Telugu",
    "th", "Thai",
    "bo", "Tibetan",
    "ti", "Tigrinya",
    "to", "Tonga",
    "ts", "Tsonga",
    "tn", "Tswana",
    "tr", "Turkish",
    "tk", "Turkmen",
    "tw", "Twi",
    "ug", "Uighur",
    "uk", "Ukrainian",
    "ur", "Urdu",
    "uz", "Uzbek",
    "ve", "Venda",
    "vi", "Vietnamese",
    "vo", "Volapük",
    "wa", "Walloon",
    "cy", "Welsh",
    "fy", "Western Frisian",
    "wo", "Wolof",
    "xh", "Xhosa",
    "yi", "Yiddish",
    "yo", "Yoruba",
    "za", "Zhuang",
    "zu", "Zulu"
  };

  /** ISO 639-2/B codes that differ from the terminology codes {@link Locale} knows. */
  private static final String[] BIBLIOGRAPHIC = {
    "alb", "sq", "arm", "hy", "baq", "eu", "bur", "my", "chi", "zh", "cze", "cs", "dut", "nl",
    "fre", "fr", "geo", "ka", "ger", "de", "gre", "el", "ice", "is", "mac", "mk", "mao", "mi",
    "may", "ms", "per", "fa", "rum", "ro", "slo", "sk", "tib", "bo", "wel", "cy"
  };

  private static final Map<String, Language> BY_CODE = new HashMap<>();
  private static final Map<Language, String> TO_CODE = new HashMap<>();

  static {
    for (int i = 0; i < CODES.length; i += 2) {
      String code = CODES[i];
      Language language = Language.fromXml(CODES[i + 1]);
      BY_CODE.put(code, language);
      TO_CODE.putIfAbsent(language, code);
      try {
        BY_CODE.putIfAbsent(Locale.of(code).getISO3Language(), language);
      } catch (MissingResourceException e) {
        // No three-letter code known for it.
      }
    }
    for (int i = 0; i < BIBLIOGRAPHIC.length; i += 2) {
      Language language = BY_CODE.get(BIBLIOGRAPHIC[i + 1]);
      if (language != null) {
        BY_CODE.putIfAbsent(BIBLIOGRAPHIC[i], language);
      }
    }
  }

  private Languages() {}

  /** The PAGE language of a code, or null if the code is unknown. */
  public static @Nullable Language fromCode(String code) {
    String language = code.strip().split("[-_]", 2)[0].toLowerCase(Locale.ROOT);
    return BY_CODE.get(language);
  }

  /** The PAGE language of a code; an unknown code is reported and read as {@code other}. */
  public static Language read(String code, Attrs at, String attribute) {
    Language language = fromCode(code);
    if (language == null) {
      at.changed(attribute + " '" + code + "' is not a known language code, read as other");
      return Language.OTHER;
    }
    return language;
  }

  /** The two-letter code of a PAGE language, or null if it has none. */
  public static @Nullable String toCode(Language language) {
    return TO_CODE.get(language);
  }
}
