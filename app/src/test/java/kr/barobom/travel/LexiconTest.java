package kr.barobom.travel;
import org.junit.*;
import java.io.*;
import static org.junit.Assert.*;
public class LexiconTest {
    private Lexicon lexicon;
    @Before public void load() throws Exception {lexicon=new Lexicon(new FileInputStream("src/main/assets/lexicon.tsv"));}
    @Test public void menuAndPrice(){assertEquals("바지락 술찜",lexicon.exact("あさりの酒蒸し ６８０円").korean);assertEquals("돈가스 덮밥",lexicon.exact("カツ丼 ￥1,200").korean);}
    @Test public void standaloneTaxIsPreserved(){assertEquals("세금 포함",lexicon.exact("税込").korean);assertEquals("세금 별도",lexicon.exact("税抜").korean);}
    @Test public void productLabels(){assertEquals("리필용",lexicon.exact("つめかえ用").korean);assertEquals("가열 조리용",lexicon.exact("加熱用").korean);assertEquals("생식용",lexicon.exact("生食用").korean);}
    @Test public void dontInventBrandMeaning(){assertNull(lexicon.exact("謎のブランド 新しい商品"));assertNull(lexicon.exact("キュキュット"));assertEquals("",lexicon.hints("ミラクルZ"));assertTrue(lexicon.glossary("ゆず香る鶏白湯らーめん").contains("유자"));}
    @Test public void compoundIsNotTreatedAsExact(){assertNull(lexicon.exact("季節の野菜と鶏肉のスープ"));assertTrue(lexicon.hints("季節の野菜と鶏肉のスープ").contains("닭고기"));}
    @Test public void productNumbersAreNotPrices(){assertEquals("ビタミンB12",Lexicon.normalize("ビタミンB12"));assertEquals("ラーメン2",Lexicon.normalize("ラーメン2"));assertEquals("ラーメン",Lexicon.normalize("ラーメン 880"));}
    @Test public void protectedFoodNamesCannotBeInvented(){Lexicon.ProtectedText p=lexicon.protect("数量限定の炙りサーモン丼");assertTrue(p.source.contains("__TERM0__"));assertTrue(p.restore("__TERM1__의 __TERM0__").contains("겉을 살짝 구운 연어 덮밥"));assertEquals("",p.restore("수량 한정의 구운 연어 접시"));}
    @Test public void protectedWordsKeepNegationAndRepeatCounts(){Lexicon.ProtectedText p=lexicon.protect("つめかえ用ではありません");assertTrue(p.source.endsWith("ではありません"));assertTrue(p.restore("__TERM0__이 아닙니다").endsWith("아닙니다"));Lexicon.ProtectedText repeated=lexicon.protect("サーモンとサーモン");assertEquals("",repeated.restore("__TERM0__"));assertEquals("연어와 연어",repeated.restore("__TERM0__와 __TERM0__"));}
    @Test public void menuHasBreadth(){assertTrue(lexicon.size()>350);assertEquals("닭고기 대파 꼬치",lexicon.exact("ねぎま").korean);assertEquals("곱창 전골",lexicon.exact("もつ鍋").korean);}
    @Test public void normalizesWidthAndWhitespace(){assertEquals("고로케",lexicon.exact(" ｺﾛｯｹ ").korean);assertEquals("고등어 된장조림",lexicon.exact("サバ の 味噌煮 880円(税込)").korean);}
    @Test public void ambiguousPiecesDontBecomeClaims(){assertEquals("달걀 미사용 표기",lexicon.exact("卵不使用").korean);assertNull(lexicon.exact("食器用洗剤ではありません"));}
}
