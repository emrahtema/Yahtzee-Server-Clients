package game;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import yahtzee_client.Client;

public class Yahtzee extends javax.swing.JFrame {

    //framedeki komponentlere erişim için satatik oyun değişkeni
    public static Yahtzee ThisGame;
    //ekrandaki resim değişimi için timer yerine thread
    public Thread tmr_slider;
    //zar resimleri için dizi
    ImageIcon zar_resimleri[];
    //bizim ve rakibin gösterilen puan değerlerini tutacak diziler.
    private int[] skor = new int[16];
    private int[] rakipSkor = new int[16];
    //bizim elimizdeki zarların değerlerini tutacak dizi
    private int[] elindekiZarlarinDegerleri = new int[5];
    //masadaki zarların değerlerini tutacak olan dizi
    //zar varsa 1,2,3,4,5,6 eğer yoksa 0
    private int[] ortadakiZarlarinDegerleri = new int[5];
    //serverdan yada rakipten gelen mesajlar direkt bu değişkene geliyor.
    public String gelenMesaj = "";
    //bir el 3 kere zar atmayla oynanıyor ve tabi bunun öncesi ve sonrası da var
    //oyun sırası bizdeyken oyunun hangi adımında olduğumuzun kaydını tutuyor.
    public int islemSirasi = 0;
    Operations op;
    //üst puanımız 65 ve üstüyse +30 puan bonus geliyor ve bu ekstra puan
    //65 puan tamamlanır tamamlanmaz ekleniyor. Tamamlanıp tamamlanmadığı bilgisini
    //sayac modeliyle tutan değişken.
    private int ustPuanSayac = 0;
    //bizim yada rakibin oyununun bitip bitmediğinin kontrolünü yapacağımız değişkenler
    //toplamda 14 kere oynadıysak bütün puanlar bitmiş olmalı. Bunu sayaç gibi tutuyorlar.
    private int bittiMi = 0, rakipBittiMi = 0;

    public Yahtzee() {
        initComponents();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/icon.png")));
        op = new Operations();
        ThisGame = this;
        try {
            //zar resimlerini tutacak olan diziye bütün zar resimlerini yüklüyoruz.
            //sıfırıncı zar boş resim demek hiç bir şey göstermiyor yani zar yok demek
            //Zar yoksa labelde boş resim gösteriyoruz, zar yokmuş gibi görünüyor.
            zar_resimleri = new ImageIcon[7];
            zar_resimleri[0] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/none.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            zar_resimleri[1] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/1.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            zar_resimleri[2] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/2.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            zar_resimleri[3] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/3.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            zar_resimleri[4] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/4.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            zar_resimleri[5] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/5.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            zar_resimleri[6] = new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/6.png"))).getImage().getScaledInstance(75, 75, Image.SCALE_DEFAULT));
            //Zarla butonuna resim koyuyoruz.
            zarla.setIcon(new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/dice.png"))).getImage().getScaledInstance(150, 50, Image.SCALE_DEFAULT)));
            //tablonun labeli. Buna puan tablosu resmini yüklüyoruz.
            jLabel1.setIcon(new ImageIcon(new ImageIcon(ImageIO.read(this.getClass().getResource("/images/table.png"))).getImage().getScaledInstance(302, 573, Image.SCALE_DEFAULT)));
        } catch (Exception e) {
            System.out.println(e);
        }

        //rakipten yada serverden mesaj gelip gelmediğini ve oyunun akışının kontrolünü
        //takip etmesi için thread kullanıyoruz.
        tmr_slider = new Thread(() -> {
            //soket bağlıysa dönsün
            while (Client.socket.isConnected()) {
                try {
                    //while 0,1 saniyede bir dönsün fazla işlem yapmasın.
                    Thread.sleep(100);
                    //eğer bir mesaj geldiyse bu string boş değildir.
                    //boş değilse de aşağıdaki işlemleri yapsın ve boş olsun.
                    if (!gelenMesaj.equals("")) {
                        //birden çok mesaj varsa bölelim.
                        String[] msg = gelenMesaj.split("~");
                        gelenMesaj = "";
                        for (String str : msg) {
                            //her mesajın içeriğini bölelim.
                            //mesaj Start ile başlıyorsa oyuna kim başlayacak
                            //yada oyun sırası kimde onu söylüyor.
                            //oynama hakkı 1 ise bizde, 0 ise rakiptedir.
                            if (str.equals("Start|1")) {
                                islemSirasi = 1;
                                //işlem sırasını güncelleyip bütün zar resimlerini sıfırlıyoruz.
                                butun_zarlari_sifirla();
                            }else if(str.equals("Start|0")){
                                islemSirasi = 0;
                                butun_zarlari_sifirla();
                            }else if (str.charAt(0) == 'P') {
                                //rakip elini oynayıp bitirmiş ve bir puan seçmişse
                                //bize bu bilgiyi içeren bir mesaj gönderiyor
                                //bu mesaj da P ile başlıyor.
                                //Bütün puanlar geldiğinden onları ayırıyoruz
                                //ve ekranda rakibin puanlarını bunlara göre güncelliyoruz.
                                //neden string bölme metotları kullanmadık dersek
                                //nedense bu kısımda çalışmıyorlar, bölemiyoruz.
                                int index = 0;
                                String puan = "";
                                for (int i = 2; i < str.length(); i++) {
                                    if (str.charAt(i) == '+') {
                                        rakipSkor[index] = Integer.parseInt(puan);
                                        index++;
                                        puan = "";
                                    } else {
                                        puan = puan + str.charAt(i);
                                    }
                                }
                                //rakibin puanlarının gösterildiği bölge güncellensin.
                                rakip_puanlarini_goster();
                            } else if (str.charAt(0) == 'Z'){
                                //Z ile başlayan bir mesaj gelmişse rakip bir zara tıklayıp
                                //işlem yapmış demektir. Bu tıkladığı zarın işlemini konumunu
                                //görebilmemiz için bize kendi elinin ve masanın zar gösterimlerini
                                //yolluyor. Bunları metoda gönderip çözümlüyoruz ve ekranda gösteriyoruz
                                //böylece rakibin her adımını takip edebiliyoruz.
                                rakibin_oynayisi_goster(str);
                            }
                            else if(str.equals("Start|-1") && bittiMi!=13 && rakipBittiMi!=13){
                                //rakip oyundan çıktıysa -1 mesajı gelir.
                                //eğer oyun bitti de rakip çıktıysa buraya hiç girmesin.
                                durum.setText("Rakip Oyundan Çıktı, Yeni oyun için yeniden başlat.");
                                islemSirasi = -1;
                                zarla.setEnabled(false);
                                break;
                            }
                        }
                    }
                    if (islemSirasi == 1) {
                        durum.setText("Durum: Oynama Sırası Sende.");
                        //ilk sıra zarlar, oyun sırası bize geldiğinde 
                        //elimizde belirecek olan zarlardır. Rasgele geleceklerdir.
                        //herhangi bir anlamları yok, masaya atıldıktan sonra anlamlılar.
                        ilk_sira_zarlari_at();
                        zarla.setEnabled(true);
                        islemSirasi = 2;
                    } else if (islemSirasi == 0) {
                        durum.setText("Durum: Oynama Sırası Rakipte.");
                    }
                    //aşağıdaki kodlar işe yaramıyor, tekrar bağlanmıyor.
                    //7 saniye sonra oyun bitsin tekrar bağlansın
                    //Thread.sleep(7000);
                    //Reset();
                } catch (Exception e) {
                    //System.out.println(e);
                }
            }
        });
    }

    //rakip bize bir puan bilgisi göndermişse, rakibin gösterilen puanlarını
    //bu metod sayesinde güncelliyoruz, ama tabi sadece seçilmiş puanları.
    //henüz seçilmemiş kısımlar boş görünüyor.
    public void rakip_puanlarini_goster() {
        if (rakippuan1.getText().equals("") && rakipSkor[0] != -1) {
            rakippuan1.setText(String.valueOf(rakipSkor[0]));
            rakippuan1.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan2.getText().equals("") && rakipSkor[1] != -1) {
            rakippuan2.setText(String.valueOf(rakipSkor[1]));
            rakippuan2.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan3.getText().equals("") && rakipSkor[2] != -1) {
            rakippuan3.setText(String.valueOf(rakipSkor[2]));
            rakippuan3.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan4.getText().equals("") && rakipSkor[3] != -1) {
            rakippuan4.setText(String.valueOf(rakipSkor[3]));
            rakippuan4.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan5.getText().equals("") && rakipSkor[4] != -1) {
            rakippuan5.setText(String.valueOf(rakipSkor[4]));
            rakippuan5.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan6.getText().equals("") && rakipSkor[5] != -1) {
            rakippuan6.setText(String.valueOf(rakipSkor[5]));
            rakippuan6.setBackground(Color.BLACK);
            rakipBittiMi++;
        }

        rakippuan7.setText(String.valueOf(rakipSkor[6]));
        rakippuan7.setBackground(Color.BLACK);

        if (rakippuan8.getText().equals("") && rakipSkor[7] != -1) {
            rakippuan8.setText(String.valueOf(rakipSkor[7]));
            rakippuan8.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan9.getText().equals("") && rakipSkor[8] != -1) {
            rakippuan9.setText(String.valueOf(rakipSkor[8]));
            rakippuan9.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan10.getText().equals("") && rakipSkor[9] != -1) {
            rakippuan10.setText(String.valueOf(rakipSkor[9]));
            rakippuan10.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan11.getText().equals("") && rakipSkor[10] != -1) {
            rakippuan11.setText(String.valueOf(rakipSkor[10]));
            rakippuan11.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan12.getText().equals("") && rakipSkor[11] != -1) {
            rakippuan12.setText(String.valueOf(rakipSkor[11]));
            rakippuan12.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan13.getText().equals("") && rakipSkor[12] != -1) {
            rakippuan13.setText(String.valueOf(rakipSkor[12]));
            rakippuan13.setBackground(Color.BLACK);
            rakipBittiMi++;
        }
        if (rakippuan14.getText().equals("") && rakipSkor[13] != -1) {
            rakippuan14.setText(String.valueOf(rakipSkor[13]));
            rakippuan14.setBackground(Color.BLACK);
            rakipBittiMi++;
        }

        rakippuan15.setText(String.valueOf(rakipSkor[14]));
        rakippuan16.setText(String.valueOf(rakipSkor[15]));
        rakippuan15.setBackground(Color.BLACK);
        rakippuan16.setBackground(Color.BLACK);

        //oyun bitti mi diye kontrol etmek lazım belki rakip bitirdi
        //ve bize son puan durumunu gönderdi. Oynayamamamız lazım.
        bitis_kontrol();
    }

    //elimizdeki zarlara göre puanları hesaplayıp getiren metod.
    //her zar oynattığımızda kontrolü yapılıyor ama sadece zarların
    //hepsi elimizdeyse puanları getiriyor. Yani 3 kere zar attıktan sonra
    //puanları görebilmek için tüm zarları elimize toplamak lazım.
    public void zarlara_gore_puanlari_getir() {
        op = new Operations();
        int[] puanlar = op.puanlariGetir(elindekiZarlarinDegerleri);
        if (skor[0] == -1) {
            benpuan1.setText(String.valueOf(puanlar[0]));
            benpuan1.setBackground(Color.yellow);
        }
        if (skor[1] == -1) {
            benpuan2.setText(String.valueOf(puanlar[1]));
            benpuan2.setBackground(Color.yellow);
        }
        if (skor[2] == -1) {
            benpuan3.setText(String.valueOf(puanlar[2]));
            benpuan3.setBackground(Color.yellow);
        }
        if (skor[3] == -1) {
            benpuan4.setText(String.valueOf(puanlar[3]));
            benpuan4.setBackground(Color.yellow);
        }
        if (skor[4] == -1) {
            benpuan5.setText(String.valueOf(puanlar[4]));
            benpuan5.setBackground(Color.yellow);
        }
        if (skor[5] == -1) {
            benpuan6.setText(String.valueOf(puanlar[5]));
            benpuan6.setBackground(Color.yellow);
        }

        benpuan7.setText(String.valueOf(skor[6]));

        if (skor[7] == -1) {
            benpuan8.setText(String.valueOf(puanlar[6]));
            benpuan8.setBackground(Color.yellow);
        }
        if (skor[8] == -1) {
            benpuan9.setText(String.valueOf(puanlar[7]));
            benpuan9.setBackground(Color.yellow);
        }
        if (skor[9] == -1) {
            benpuan10.setText(String.valueOf(puanlar[8]));
            benpuan10.setBackground(Color.yellow);
        }
        if (skor[10] == -1) {
            benpuan11.setText(String.valueOf(puanlar[9]));
            benpuan11.setBackground(Color.yellow);
        }
        if (skor[11] == -1) {
            benpuan12.setText(String.valueOf(puanlar[10]));
            benpuan12.setBackground(Color.yellow);
        }
        if (skor[12] == -1) {
            benpuan13.setText(String.valueOf(puanlar[11]));
            benpuan13.setBackground(Color.yellow);
        }
        if (skor[13] == -1) {
            benpuan14.setText(String.valueOf(puanlar[12]));
            benpuan14.setBackground(Color.yellow);
        } else if (skor[13] != -1 && puanlar[12] != 0) {
            benpuan14.setText("100");
            benpuan14.setBackground(Color.yellow);
        }

        benpuan15.setText(String.valueOf(skor[14]));
        benpuan16.setText(String.valueOf(skor[15]));
    }

    //bir puan seçildikten sonra puan seçme labellerinde hala seçilmeyen puanlar
    //görünüyor. Bunları sıfırlamak için bu metod kullanılıyor.
    public void puansiz_labelleri_sifirla() {
        if (skor[0] == -1) {
            benpuan1.setText("");
            benpuan1.setBackground(Color.white);
        }
        if (skor[1] == -1) {
            benpuan2.setText("");
            benpuan2.setBackground(Color.white);
        }
        if (skor[2] == -1) {
            benpuan3.setText("");
            benpuan3.setBackground(Color.white);
        }
        if (skor[3] == -1) {
            benpuan4.setText("");
            benpuan4.setBackground(Color.white);
        }
        if (skor[4] == -1) {
            benpuan5.setText("");
            benpuan5.setBackground(Color.white);
        }
        if (skor[5] == -1) {
            benpuan6.setText("");
            benpuan6.setBackground(Color.white);
        }
        if (skor[7] == -1) {
            benpuan8.setText("");
            benpuan8.setBackground(Color.white);
        }
        if (skor[8] == -1) {
            benpuan9.setText("");
            benpuan9.setBackground(Color.white);
        }
        if (skor[9] == -1) {
            benpuan10.setText("");
            benpuan10.setBackground(Color.white);
        }
        if (skor[10] == -1) {
            benpuan11.setText("");
            benpuan11.setBackground(Color.white);
        }
        if (skor[11] == -1) {
            benpuan12.setText("");
            benpuan12.setBackground(Color.white);
        }
        if (skor[12] == -1) {
            benpuan13.setText("");
            benpuan13.setBackground(Color.white);
        }
        if (skor[13] == -1) {
            benpuan14.setText("");
            benpuan14.setBackground(Color.white);
        }
    }

    //eğer masada hiç zar kalmamışsa, iyi kötü hepsini elimize almışsak
    //bunu kontrol edip tespit ediyoruz.
    //eğer tespit edilirse, elimizdeki zarlara göre seçebileceğimiz
    //puanları getirecek metodu çağırıyoruz.
    public void kontrol_et() {
        puansiz_labelleri_sifirla();
        int sayac = 0;
        for (int i : ortadakiZarlarinDegerleri) {
            sayac += i;
        }
        if (sayac == 0 && islemSirasi > 2 && islemSirasi < 6) {
            zarlara_gore_puanlari_getir();
        }
    }

    //biz her oynadığımızda zarların hem yerleri hem değerleri değişebiliyor. Bütün bu değişiklikler için
    //bu metod çağırılıyor ve zarların değerlerine göre masadaki zarların resimleri güncelliyor.
    public void masadaki_zarlarin_resimlerini_guncelle(boolean kordinatDegistir) {
        masazar1.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[0]]);
        masazar2.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[1]]);
        masazar3.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[2]]);
        masazar4.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[3]]);
        masazar5.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[4]]);

        if(kordinatDegistir){
            masazar1.setLocation(op.addToX(), op.addToY());
            masazar2.setLocation(100 + op.addToX(), op.addToY());
            masazar3.setLocation(200 + op.addToX(), op.addToY());
            masazar4.setLocation(300 + op.addToX(), op.addToY());
            masazar5.setLocation(400 + op.addToX(), op.addToY());
        }
    }

    //masadaki zarların resimlerini güncelleyen metod olur da elimizdekileri güncelleyen olmaz mı.
    public void eldeki_zarlarin_resimlerini_guncelle() {
        benzar1.setIcon(zar_resimleri[elindekiZarlarinDegerleri[0]]);
        benzar2.setIcon(zar_resimleri[elindekiZarlarinDegerleri[1]]);
        benzar3.setIcon(zar_resimleri[elindekiZarlarinDegerleri[2]]);
        benzar4.setIcon(zar_resimleri[elindekiZarlarinDegerleri[3]]);
        benzar5.setIcon(zar_resimleri[elindekiZarlarinDegerleri[4]]);
    }

    //önce masadaki zarlara rasgele değerler alalım.
    //sonra bu değerlere ait olan zar resimlerini alalım.
    //masadaki zar null ise kenara ayırmış elimize almış olabiliriz
    //o yüzden değeri null olmayan zarları atıyoruz.
    public void zarlari_at(){
        if (ortadakiZarlarinDegerleri[0] != 0) {
            ortadakiZarlarinDegerleri[0] = op.randomZarGetir();
        }
        if (ortadakiZarlarinDegerleri[1] != 0) {
            ortadakiZarlarinDegerleri[1] = op.randomZarGetir();
        }
        if (ortadakiZarlarinDegerleri[2] != 0) {
            ortadakiZarlarinDegerleri[2] = op.randomZarGetir();
        }
        if (ortadakiZarlarinDegerleri[3] != 0) {
            ortadakiZarlarinDegerleri[3] = op.randomZarGetir();
        }
        if (ortadakiZarlarinDegerleri[4] != 0) {
            ortadakiZarlarinDegerleri[4] = op.randomZarGetir();
        }
        masadaki_zarlarin_resimlerini_guncelle(true);
    }

    //ilk olarak zarlar elimizde oluyor. Bunları ilk defa masaya atacağız.
    //bu işlemi aşağıdaki metod yapıyor.
    public void elindeki_zarlari_at() {
        ortadakiZarlarinDegerleri[0] = op.randomZarGetir();
        ortadakiZarlarinDegerleri[1] = op.randomZarGetir();
        ortadakiZarlarinDegerleri[2] = op.randomZarGetir();
        ortadakiZarlarinDegerleri[3] = op.randomZarGetir();
        ortadakiZarlarinDegerleri[4] = op.randomZarGetir();
        masadaki_zarlarin_resimlerini_guncelle(true);

        elindekiZarlarinDegerleri[0] = 0;
        elindekiZarlarinDegerleri[1] = 0;
        elindekiZarlarinDegerleri[2] = 0;
        elindekiZarlarinDegerleri[3] = 0;
        elindekiZarlarinDegerleri[4] = 0;
        eldeki_zarlarin_resimlerini_guncelle();
    }

    //Bu metod sıra bize geldiğinde ilk olarak bütün zarların rasgele 
    //değerlerle elimizde görülmesini sağlıyor. Aslında zarların atılana
    //kadar bir değerleri yok.
    public void ilk_sira_zarlari_at() {
        elindekiZarlarinDegerleri[0] = op.randomZarGetir();
        elindekiZarlarinDegerleri[1] = op.randomZarGetir();
        elindekiZarlarinDegerleri[2] = op.randomZarGetir();
        elindekiZarlarinDegerleri[3] = op.randomZarGetir();
        elindekiZarlarinDegerleri[4] = op.randomZarGetir();
        ortadakiZarlarinDegerleri[0] = 0;
        ortadakiZarlarinDegerleri[1] = 0;
        ortadakiZarlarinDegerleri[2] = 0;
        ortadakiZarlarinDegerleri[3] = 0;
        ortadakiZarlarinDegerleri[4] = 0;
        eldeki_zarlarin_resimlerini_guncelle();
    }

    //oyun sıramız bittiğinde herşeyi sıfırlayan metod.
    //zarlar görülmesin gereksiz yere.
    public void butun_zarlari_sifirla() {
        rakipzar1.setIcon(zar_resimleri[0]);
        rakipzar2.setIcon(zar_resimleri[0]);
        rakipzar3.setIcon(zar_resimleri[0]);
        rakipzar4.setIcon(zar_resimleri[0]);
        rakipzar5.setIcon(zar_resimleri[0]);

        for (int i = 0; i < 5; i++) {
            elindekiZarlarinDegerleri[i] = 0;
            ortadakiZarlarinDegerleri[i] = 0;
        }
        masadaki_zarlarin_resimlerini_guncelle(true);
        eldeki_zarlarin_resimlerini_guncelle();
    }

    //eğer bir puan seçmişsek bu puanın bizim skor dizimize kaydedilmesi gerekiyor
    //seçtiğimiz puanı diziye ekliyor ve üzerinde seçilmek için puan görünen 
    //ama aslında puanı olmayan labelleri sıfırlıyor.
    public void puani_ekle(int puan, int index) {
        skor[index] = puan;
        zarla.setEnabled(false);

        if (skor[0] == -1) {
            benpuan1.setText("");
            benpuan1.setBackground(Color.white);
            ustPuanSayac++;
        } else if (index == 0) {
            benpuan1.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[1] == -1) {
            benpuan2.setText("");
            benpuan2.setBackground(Color.white);
            ustPuanSayac++;
        } else if (index == 1) {
            benpuan2.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[2] == -1) {
            benpuan3.setText("");
            benpuan3.setBackground(Color.white);
            ustPuanSayac++;
        } else if (index == 2) {
            benpuan3.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[3] == -1) {
            benpuan4.setText("");
            benpuan4.setBackground(Color.white);
            ustPuanSayac++;
        } else if (index == 3) {
            benpuan4.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[4] == -1) {
            benpuan5.setText("");
            benpuan5.setBackground(Color.white);
            ustPuanSayac++;
        } else if (index == 4) {
            benpuan5.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[5] == -1) {
            benpuan6.setText("");
            benpuan6.setBackground(Color.white);
            ustPuanSayac++;
        } else if (index == 5) {
            benpuan6.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[7] == -1) {
            benpuan8.setText("");
            benpuan8.setBackground(Color.white);
        } else if (index == 7) {
            benpuan8.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[8] == -1) {
            benpuan9.setText("");
            benpuan9.setBackground(Color.white);
        } else if (index == 8) {
            benpuan9.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[9] == -1) {
            benpuan10.setText("");
            benpuan10.setBackground(Color.white);
        } else if (index == 9) {
            benpuan10.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[10] == -1) {
            benpuan11.setText("");
            benpuan11.setBackground(Color.white);
        } else if (index == 10) {
            benpuan11.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[11] == -1) {
            benpuan12.setText("");
            benpuan12.setBackground(Color.white);
        } else if (index == 11) {
            benpuan12.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[12] == -1) {
            benpuan13.setText("");
            benpuan13.setBackground(Color.white);
        } else if (index == 12) {
            benpuan13.setBackground(Color.green);
            bittiMi++;
        }
        if (skor[13] == -1) {
            benpuan14.setText("");
            benpuan14.setBackground(Color.white);
        } else if (index == 13) {
            benpuan14.setBackground(Color.green);
            bittiMi++;
        }

        int ustPuan = 0;
        for (int i = 0; i < 6; i++) {
            if (skor[i] != -1) {
                ustPuan += skor[i];
            }
        }

        int altPuan = 0;
        for (int i = 7; i < 14; i++) {
            if (skor[i] != -1) {
                altPuan += skor[i];
            }
        }

        if (ustPuanSayac == 6) {
            if (ustPuan >= 65) {
                ustPuan += 30;
                ustPuanSayac = 7;
            }
        }
        int genelPuan = ustPuan + altPuan;
        skor[6] = ustPuan;
        skor[14] = altPuan;
        skor[15] = genelPuan;
        benpuan7.setText(String.valueOf(ustPuan));
        benpuan15.setText(String.valueOf(altPuan));
        benpuan16.setText(String.valueOf(genelPuan));

        islemSirasi = 0;
        String rakibeMesaj = "Start|1~P|";
        for (int i : skor) {
            rakibeMesaj = rakibeMesaj + String.valueOf(i) + "+";
        }

        butun_zarlari_sifirla();
        //mesajı rakibe gönderelim.
        Message msg = new Message(Message.Message_Type.Play);
        msg.content = rakibeMesaj;
        Client.Send(msg);
        bitis_kontrol();
    }

    //oyunun bitip bitmediğinin kontrolünü yapıyor.
    public void bitis_kontrol() {
        if (bittiMi == 13 && rakipBittiMi == 13) {
            kim_kazandi();
            zarla.setEnabled(false);
            islemSirasi = -1;
            durum.setText("Oyun Bitti. Yeni oyun için yeniden başlatın.");
            Message msg = new Message(Message.Message_Type.Bitis);
            Client.Send(msg);
            baglanbutton.setEnabled(true);
        }
    }

    //oyun bitmişse kim kazanmış bunu belirleyen metod.
    public void kim_kazandi() {
        String kazanan = "";
        if (Integer.parseInt(benpuan16.getText()) > Integer.parseInt(rakippuan16.getText())) {
            kazanan = "SEN";
        } else if (Integer.parseInt(benpuan16.getText()) < Integer.parseInt(rakippuan16.getText())) {
            kazanan = "RAKİP";
        } else {
            kazanan = "BERABERE";
        }
        JOptionPane.showMessageDialog(null, ("Puanın: " + benpuan16.getText() + " Rakip: " + rakippuan16.getText() + " \nKazanan: " + kazanan), "OYUN BİTTİ", JOptionPane.INFORMATION_MESSAGE);
        Reset();
    }

    public void Reset() {
        if (Client.socket != null) {
            if (Client.socket.isConnected()) {
                Client.Stop();
            }
        }
    }

    //bir zarı hareket ettirmişsek rakibe bunun bilgisini gönderiyoruz ki
    //rakip sürekli olarak oynayış adımlarımızı görebilsin.
    public void rakibe_oynadigini_gonder() {
        Message msg = new Message(Message.Message_Type.Play);
        msg.content = "Z|"
                + ortadakiZarlarinDegerleri[0]
                + ortadakiZarlarinDegerleri[1]
                + ortadakiZarlarinDegerleri[2]
                + ortadakiZarlarinDegerleri[3]
                + ortadakiZarlarinDegerleri[4]
                + elindekiZarlarinDegerleri[0]
                + elindekiZarlarinDegerleri[1]
                + elindekiZarlarinDegerleri[2]
                + elindekiZarlarinDegerleri[3]
                + elindekiZarlarinDegerleri[4];
        Client.Send(msg);
    }

    //rakibin oynadığı adımları gösteren metod.
    public void rakibin_oynayisi_goster(String zarlar) {
        ortadakiZarlarinDegerleri[0] = Integer.parseInt(zarlar.charAt(6) + "");
        ortadakiZarlarinDegerleri[1] = Integer.parseInt(zarlar.charAt(5) + "");
        ortadakiZarlarinDegerleri[2] = Integer.parseInt(zarlar.charAt(4) + "");
        ortadakiZarlarinDegerleri[3] = Integer.parseInt(zarlar.charAt(3) + "");
        ortadakiZarlarinDegerleri[4] = Integer.parseInt(zarlar.charAt(2) + "");

        rakipzar1.setIcon(zar_resimleri[Integer.parseInt(zarlar.charAt(11) + "")]);
        rakipzar2.setIcon(zar_resimleri[Integer.parseInt(zarlar.charAt(10) + "")]);
        rakipzar3.setIcon(zar_resimleri[Integer.parseInt(zarlar.charAt(9) + "")]);
        rakipzar4.setIcon(zar_resimleri[Integer.parseInt(zarlar.charAt(8) + "")]);
        rakipzar5.setIcon(zar_resimleri[Integer.parseInt(zarlar.charAt(7) + "")]);

        masadaki_zarlarin_resimlerini_guncelle(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        rakippuan2 = new javax.swing.JLabel();
        rakippuan1 = new javax.swing.JLabel();
        rakippuan16 = new javax.swing.JLabel();
        rakippuan15 = new javax.swing.JLabel();
        rakippuan14 = new javax.swing.JLabel();
        rakippuan13 = new javax.swing.JLabel();
        rakippuan12 = new javax.swing.JLabel();
        rakippuan11 = new javax.swing.JLabel();
        rakippuan10 = new javax.swing.JLabel();
        rakippuan9 = new javax.swing.JLabel();
        rakippuan8 = new javax.swing.JLabel();
        rakippuan7 = new javax.swing.JLabel();
        rakippuan6 = new javax.swing.JLabel();
        rakippuan5 = new javax.swing.JLabel();
        rakippuan4 = new javax.swing.JLabel();
        rakippuan3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        benpuan1 = new javax.swing.JLabel();
        benpuan2 = new javax.swing.JLabel();
        benpuan3 = new javax.swing.JLabel();
        benpuan4 = new javax.swing.JLabel();
        benpuan5 = new javax.swing.JLabel();
        benpuan6 = new javax.swing.JLabel();
        benpuan7 = new javax.swing.JLabel();
        benpuan8 = new javax.swing.JLabel();
        benpuan9 = new javax.swing.JLabel();
        benpuan10 = new javax.swing.JLabel();
        benpuan11 = new javax.swing.JLabel();
        benpuan12 = new javax.swing.JLabel();
        benpuan13 = new javax.swing.JLabel();
        benpuan14 = new javax.swing.JLabel();
        benpuan15 = new javax.swing.JLabel();
        benpuan16 = new javax.swing.JLabel();
        masaPanel = new javax.swing.JPanel();
        masazar1 = new javax.swing.JLabel();
        masazar2 = new javax.swing.JLabel();
        masazar3 = new javax.swing.JLabel();
        masazar4 = new javax.swing.JLabel();
        masazar5 = new javax.swing.JLabel();
        bizimPanel = new javax.swing.JPanel();
        benzar2 = new javax.swing.JLabel();
        benzar1 = new javax.swing.JLabel();
        benzar3 = new javax.swing.JLabel();
        benzar4 = new javax.swing.JLabel();
        benzar5 = new javax.swing.JLabel();
        rakipPanel = new javax.swing.JPanel();
        rakipzar1 = new javax.swing.JLabel();
        rakipzar2 = new javax.swing.JLabel();
        rakipzar3 = new javax.swing.JLabel();
        rakipzar4 = new javax.swing.JLabel();
        rakipzar5 = new javax.swing.JLabel();
        baglanbutton = new javax.swing.JButton();
        durum = new javax.swing.JLabel();
        zarla = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Yahtzee");
        setMaximumSize(new java.awt.Dimension(920, 650));
        setMinimumSize(new java.awt.Dimension(920, 650));
        setPreferredSize(new java.awt.Dimension(920, 650));
        setSize(new java.awt.Dimension(920, 650));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setMaximumSize(new java.awt.Dimension(380, 573));
        jPanel1.setMinimumSize(new java.awt.Dimension(380, 573));
        jPanel1.setPreferredSize(new java.awt.Dimension(380, 575));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2, -1, -1));

        rakippuan2.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan2.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan2.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan2.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan2, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 71, -1, -1));

        rakippuan1.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan1.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan1.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan1.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan1, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 40, -1, -1));

        rakippuan16.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan16.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan16.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan16.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan16.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan16.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan16, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 542, -1, -1));

        rakippuan15.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan15.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan15.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan15.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan15.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan15.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan15, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 510, -1, -1));

        rakippuan14.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan14.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan14.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan14.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan14.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan14.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan14, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 468, -1, -1));

        rakippuan13.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan13.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan13.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan13.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan13.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan13.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan13, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 436, -1, -1));

        rakippuan12.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan12.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan12.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan12.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan12.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan12.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan12, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 405, -1, -1));

        rakippuan11.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan11.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan11.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan11.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan11.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan11.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan11, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 372, -1, -1));

        rakippuan10.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan10.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan10.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan10.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan10.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan10.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan10, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 340, -1, -1));

        rakippuan9.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan9.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan9.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan9.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan9.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan9.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan9, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 308, -1, -1));

        rakippuan8.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan8.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan8.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan8.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan8.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan8.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan8, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 276, -1, -1));

        rakippuan7.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan7.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan7.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan7.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan7.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan7.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan7, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 242, -1, -1));

        rakippuan6.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan6.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan6.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan6.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan6.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan6.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan6, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 198, -1, -1));

        rakippuan5.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan5.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan5.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan5.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan5.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan5, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 166, -1, -1));

        rakippuan4.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan4.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan4.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan4.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan4.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan4, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 133, -1, -1));

        rakippuan3.setBackground(new java.awt.Color(153, 153, 153));
        rakippuan3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        rakippuan3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakippuan3.setMaximumSize(new java.awt.Dimension(30, 31));
        rakippuan3.setMinimumSize(new java.awt.Dimension(30, 31));
        rakippuan3.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(rakippuan3, new org.netbeans.lib.awtextra.AbsoluteConstraints(342, 102, -1, -1));

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("Ben  Rakip");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 10, -1, 30));

        benpuan1.setBackground(new java.awt.Color(255, 255, 255));
        benpuan1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan1.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan1.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan1.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan1MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan1, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 40, -1, -1));

        benpuan2.setBackground(new java.awt.Color(255, 255, 255));
        benpuan2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan2.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan2.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan2.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan2MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan2, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 71, -1, -1));

        benpuan3.setBackground(new java.awt.Color(255, 255, 255));
        benpuan3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan3.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan3.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan3.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan3MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan3, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 102, -1, -1));

        benpuan4.setBackground(new java.awt.Color(255, 255, 255));
        benpuan4.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan4.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan4.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan4.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan4MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan4, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 133, -1, -1));

        benpuan5.setBackground(new java.awt.Color(255, 255, 255));
        benpuan5.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan5.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan5.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan5.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan5MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan5, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 166, -1, -1));

        benpuan6.setBackground(new java.awt.Color(255, 255, 255));
        benpuan6.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan6.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan6.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan6.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan6.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan6MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan6, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 198, -1, -1));

        benpuan7.setBackground(new java.awt.Color(255, 255, 255));
        benpuan7.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan7.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan7.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan7.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan7.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(benpuan7, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 242, -1, -1));

        benpuan8.setBackground(new java.awt.Color(255, 255, 255));
        benpuan8.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan8.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan8.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan8.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan8.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan8MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan8, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 276, -1, -1));

        benpuan9.setBackground(new java.awt.Color(255, 255, 255));
        benpuan9.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan9.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan9.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan9.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan9.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan9MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan9, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 308, -1, -1));

        benpuan10.setBackground(new java.awt.Color(255, 255, 255));
        benpuan10.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan10.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan10.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan10.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan10.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan10MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan10, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 340, -1, -1));

        benpuan11.setBackground(new java.awt.Color(255, 255, 255));
        benpuan11.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan11.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan11.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan11.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan11.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan11MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan11, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 372, -1, -1));

        benpuan12.setBackground(new java.awt.Color(255, 255, 255));
        benpuan12.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan12.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan12.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan12.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan12.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan12MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan12, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 405, -1, -1));

        benpuan13.setBackground(new java.awt.Color(255, 255, 255));
        benpuan13.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan13.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan13.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan13.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan13.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan13.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan13MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan13, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 436, -1, -1));

        benpuan14.setBackground(new java.awt.Color(255, 255, 255));
        benpuan14.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan14.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan14.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan14.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan14.setPreferredSize(new java.awt.Dimension(30, 31));
        benpuan14.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benpuan14MouseClicked(evt);
            }
        });
        jPanel1.add(benpuan14, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 468, -1, -1));

        benpuan15.setBackground(new java.awt.Color(255, 255, 255));
        benpuan15.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan15.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan15.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan15.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan15.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(benpuan15, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 510, -1, -1));

        benpuan16.setBackground(new java.awt.Color(255, 255, 255));
        benpuan16.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        benpuan16.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        benpuan16.setMaximumSize(new java.awt.Dimension(30, 31));
        benpuan16.setMinimumSize(new java.awt.Dimension(30, 31));
        benpuan16.setPreferredSize(new java.awt.Dimension(30, 31));
        jPanel1.add(benpuan16, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 542, -1, -1));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        masaPanel.setBackground(new java.awt.Color(0, 204, 153));
        masaPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        masaPanel.setMaximumSize(new java.awt.Dimension(500, 200));
        masaPanel.setMinimumSize(new java.awt.Dimension(500, 200));
        masaPanel.setPreferredSize(new java.awt.Dimension(500, 200));
        masaPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        masazar1.setText(".");
        masazar1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                masazar1MouseClicked(evt);
            }
        });
        masaPanel.add(masazar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, -1, -1));

        masazar2.setText(".");
        masazar2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                masazar2MouseClicked(evt);
            }
        });
        masaPanel.add(masazar2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 50, -1, -1));

        masazar3.setText(".");
        masazar3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                masazar3MouseClicked(evt);
            }
        });
        masaPanel.add(masazar3, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 50, -1, -1));

        masazar4.setText(".");
        masazar4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                masazar4MouseClicked(evt);
            }
        });
        masaPanel.add(masazar4, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 50, -1, -1));

        masazar5.setText(".");
        masazar5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                masazar5MouseClicked(evt);
            }
        });
        masaPanel.add(masazar5, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 50, -1, -1));

        getContentPane().add(masaPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 170, 500, 200));

        bizimPanel.setBackground(new java.awt.Color(255, 255, 255));
        bizimPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        bizimPanel.setEnabled(false);
        bizimPanel.setMaximumSize(new java.awt.Dimension(500, 125));
        bizimPanel.setMinimumSize(new java.awt.Dimension(500, 125));
        bizimPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        benzar2.setText(".");
        benzar2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benzar2MouseClicked(evt);
            }
        });
        bizimPanel.add(benzar2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 30, -1, -1));

        benzar1.setText(".");
        benzar1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benzar1MouseClicked(evt);
            }
        });
        bizimPanel.add(benzar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, -1, -1));

        benzar3.setText(".");
        benzar3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benzar3MouseClicked(evt);
            }
        });
        bizimPanel.add(benzar3, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 30, -1, -1));

        benzar4.setText(".");
        benzar4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benzar4MouseClicked(evt);
            }
        });
        bizimPanel.add(benzar4, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 30, -1, -1));

        benzar5.setText(".");
        benzar5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                benzar5MouseClicked(evt);
            }
        });
        bizimPanel.add(benzar5, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 30, -1, -1));

        getContentPane().add(bizimPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 445, 500, 125));

        rakipPanel.setBackground(new java.awt.Color(255, 255, 255));
        rakipPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        rakipPanel.setEnabled(false);
        rakipPanel.setMaximumSize(new java.awt.Dimension(500, 115));
        rakipPanel.setMinimumSize(new java.awt.Dimension(500, 115));
        rakipPanel.setPreferredSize(new java.awt.Dimension(500, 115));
        rakipPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        rakipzar1.setText(".");
        rakipPanel.add(rakipzar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, -1, -1));

        rakipzar2.setText(".");
        rakipPanel.add(rakipzar2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 30, -1, -1));

        rakipzar3.setText(".");
        rakipPanel.add(rakipzar3, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 30, -1, -1));

        rakipzar4.setText(".");
        rakipPanel.add(rakipzar4, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 30, -1, -1));

        rakipzar5.setText(".");
        rakipPanel.add(rakipzar5, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 30, -1, -1));

        getContentPane().add(rakipPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 50, 500, 115));

        baglanbutton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        baglanbutton.setText("Oyun Ara");
        baglanbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baglanbuttonActionPerformed(evt);
            }
        });
        getContentPane().add(baglanbutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 10, 120, 30));

        durum.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        durum.setText("Durum :");
        getContentPane().add(durum, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 20, -1, -1));

        zarla.setText("jButton1");
        zarla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zarlaActionPerformed(evt);
            }
        });
        getContentPane().add(zarla, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 383, 150, 50));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    //butona tıklandığında serverla bağlantı kuruyor.
    private void baglanbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baglanbuttonActionPerformed
        try {
            //bağlanılacak server ve portu veriyoruz
            Client.Start("127.0.0.1", 2000);
            //başlangıç durumları
            durum.setText("Durum: Rakip Bekleniyor...");
            baglanbutton.setEnabled(false);
            Message msg = new Message(Message.Message_Type.Start);
            Client.Send(msg);
        } catch (Exception e) {
            baglanbutton.setEnabled(true);
            durum.setText("Server ile bağlantı kurulamadı.");
        }

    }//GEN-LAST:event_baglanbuttonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        //form kapanırken clienti durdur
        Client.Stop();
    }//GEN-LAST:event_formWindowClosing

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        //client ilk açıldığında verilmesi gereken default değerlerin
        //atamasını yapıyoruz.
        zarla.setEnabled(false);
        //masadaki ve eldeki zarların default değerlerini atıyoruz.
        for (int i = 0; i < 5; i++) {
            elindekiZarlarinDegerleri[i] = 0;
            ortadakiZarlarinDegerleri[i] = 0;
        }
        //-1 olan bütün skorlar boş demektir.
        for (int i = 0; i < 16; i++) {
            skor[i] = -1;
            rakipSkor[i] = -1;
        }
        skor[6] = 0;
        skor[14] = 0;
        skor[15] = 0;
        rakipSkor[6] = 0;
        rakipSkor[14] = 0;
        rakipSkor[15] = 0;
        benpuan7.setText("0");
        benpuan15.setText("0");
        benpuan16.setText("0");
        benpuan7.setBackground(Color.green);
        benpuan15.setBackground(Color.green);
        benpuan16.setBackground(Color.green);

        //zarların üstünde nokta var. nokta silinirse zarları göremeyiz birdaha
        //oyüzden noktayı kod ile siliyoruz geçici olarak.
        rakipzar1.setText("");
        rakipzar2.setText("");
        rakipzar3.setText("");
        rakipzar4.setText("");
        rakipzar5.setText("");

        masazar1.setText("");
        masazar2.setText("");
        masazar3.setText("");
        masazar4.setText("");
        masazar5.setText("");

        benzar1.setText("");
        benzar2.setText("");
        benzar3.setText("");
        benzar4.setText("");
        benzar5.setText("");

    }//GEN-LAST:event_formWindowOpened

    private void zarlaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zarlaActionPerformed
        op = new Operations();
        if (islemSirasi == 2) {
            islemSirasi=3;
            elindeki_zarlari_at();
        } else if (islemSirasi == 3) {
            islemSirasi=4;
            zarlari_at();
        } else if (islemSirasi == 4) {
            islemSirasi=5;
            zarlari_at();
            zarla.setEnabled(false);
        }
        //butona tıklayıp masaya zarları attık, bunu rakibe gönderelim ki masayı görsün.
        rakibe_oynadigini_gonder();
    }//GEN-LAST:event_zarlaActionPerformed

    //zarlara tıklandığında bir takım işlemlerin yapılması gerek
    //örneğin masadaki zara tıklanmışsa elimize gelmeli ve puan kontrolü
    //gerekiyorsa yapılmalı, yada elimizdeki zara tıklanmışsa zar masaya gitmeli
    //ve her durumda oynayışımız rakibe gönderilmeli.
    private void benzar1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benzar1MouseClicked
        if (ortadakiZarlarinDegerleri[0] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            masazar1.setIcon(zar_resimleri[elindekiZarlarinDegerleri[0]]);
            ortadakiZarlarinDegerleri[0] = elindekiZarlarinDegerleri[0];
            elindekiZarlarinDegerleri[0] = 0;
            benzar1.setIcon(zar_resimleri[0]);
            masazar1.setLocation(op.addToX(), op.addToY());
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_benzar1MouseClicked

    private void benzar2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benzar2MouseClicked
        if (ortadakiZarlarinDegerleri[1] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            masazar2.setIcon(zar_resimleri[elindekiZarlarinDegerleri[1]]);
            ortadakiZarlarinDegerleri[1] = elindekiZarlarinDegerleri[1];
            elindekiZarlarinDegerleri[1] = 0;
            benzar2.setIcon(zar_resimleri[0]);
            masazar2.setLocation(100 + op.addToX(), op.addToY());
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_benzar2MouseClicked

    private void benzar3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benzar3MouseClicked
        if (ortadakiZarlarinDegerleri[2] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            masazar3.setIcon(zar_resimleri[elindekiZarlarinDegerleri[2]]);
            ortadakiZarlarinDegerleri[2] = elindekiZarlarinDegerleri[2];
            elindekiZarlarinDegerleri[2] = 0;
            benzar3.setIcon(zar_resimleri[0]);
            masazar3.setLocation(200 + op.addToX(), op.addToY());
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_benzar3MouseClicked

    private void benzar4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benzar4MouseClicked
        if (ortadakiZarlarinDegerleri[3] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            masazar4.setIcon(zar_resimleri[elindekiZarlarinDegerleri[3]]);
            ortadakiZarlarinDegerleri[3] = elindekiZarlarinDegerleri[3];
            elindekiZarlarinDegerleri[3] = 0;
            benzar4.setIcon(zar_resimleri[0]);
            masazar4.setLocation(300 + op.addToX(), op.addToY());
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_benzar4MouseClicked

    private void benzar5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benzar5MouseClicked
        if (ortadakiZarlarinDegerleri[4] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            masazar5.setIcon(zar_resimleri[elindekiZarlarinDegerleri[4]]);
            ortadakiZarlarinDegerleri[4] = elindekiZarlarinDegerleri[4];
            elindekiZarlarinDegerleri[4] = 0;
            benzar5.setIcon(zar_resimleri[0]);
            masazar5.setLocation(400 + op.addToX(), op.addToY());
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_benzar5MouseClicked

    private void masazar1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_masazar1MouseClicked
        if (elindekiZarlarinDegerleri[0] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            benzar1.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[0]]);
            elindekiZarlarinDegerleri[0] = ortadakiZarlarinDegerleri[0];
            ortadakiZarlarinDegerleri[0] = 0;
            masazar1.setIcon(zar_resimleri[0]);
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_masazar1MouseClicked

    private void masazar2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_masazar2MouseClicked
        if (elindekiZarlarinDegerleri[1] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            benzar2.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[1]]);
            elindekiZarlarinDegerleri[1] = ortadakiZarlarinDegerleri[1];
            ortadakiZarlarinDegerleri[1] = 0;
            masazar2.setIcon(zar_resimleri[0]);
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_masazar2MouseClicked

    private void masazar3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_masazar3MouseClicked
        if (elindekiZarlarinDegerleri[2] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            benzar3.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[2]]);
            elindekiZarlarinDegerleri[2] = ortadakiZarlarinDegerleri[2];
            ortadakiZarlarinDegerleri[2] = 0;
            masazar3.setIcon(zar_resimleri[0]);
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_masazar3MouseClicked

    private void masazar4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_masazar4MouseClicked
        if (elindekiZarlarinDegerleri[3] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            benzar4.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[3]]);
            elindekiZarlarinDegerleri[3] = ortadakiZarlarinDegerleri[3];
            ortadakiZarlarinDegerleri[3] = 0;
            masazar4.setIcon(zar_resimleri[0]);
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_masazar4MouseClicked

    private void masazar5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_masazar5MouseClicked
        if (elindekiZarlarinDegerleri[4] == 0 && islemSirasi > 2 && islemSirasi < 6) {
            benzar5.setIcon(zar_resimleri[ortadakiZarlarinDegerleri[4]]);
            elindekiZarlarinDegerleri[4] = ortadakiZarlarinDegerleri[4];
            ortadakiZarlarinDegerleri[4] = 0;
            masazar5.setIcon(zar_resimleri[0]);
            kontrol_et();
            rakibe_oynadigini_gonder();
        }
    }//GEN-LAST:event_masazar5MouseClicked

    //puan seçileceği zaman bunlar devreye giriyor.
    private void benpuan1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan1MouseClicked
        if (skor[0] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan1.getText()), 0);
        }
    }//GEN-LAST:event_benpuan1MouseClicked

    private void benpuan2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan2MouseClicked
        if (skor[1] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan2.getText()), 1);
        }
    }//GEN-LAST:event_benpuan2MouseClicked

    private void benpuan3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan3MouseClicked
        if (skor[2] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan3.getText()), 2);
        }
    }//GEN-LAST:event_benpuan3MouseClicked

    private void benpuan4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan4MouseClicked
        if (skor[3] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan4.getText()), 3);
        }
    }//GEN-LAST:event_benpuan4MouseClicked

    private void benpuan5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan5MouseClicked
        if (skor[4] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan5.getText()), 4);
        }
    }//GEN-LAST:event_benpuan5MouseClicked

    private void benpuan6MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan6MouseClicked
        if (skor[5] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan6.getText()), 5);
        }
    }//GEN-LAST:event_benpuan6MouseClicked

    private void benpuan8MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan8MouseClicked
        if (skor[7] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan8.getText()), 7);
        }
    }//GEN-LAST:event_benpuan8MouseClicked

    private void benpuan9MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan9MouseClicked
        if (skor[8] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan9.getText()), 8);
        }
    }//GEN-LAST:event_benpuan9MouseClicked

    private void benpuan10MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan10MouseClicked
        if (skor[9] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan10.getText()), 9);
        }
    }//GEN-LAST:event_benpuan10MouseClicked

    private void benpuan11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan11MouseClicked
        if (skor[10] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan11.getText()), 10);
        }
    }//GEN-LAST:event_benpuan11MouseClicked

    private void benpuan12MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan12MouseClicked
        if (skor[11] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan12.getText()), 11);
        }
    }//GEN-LAST:event_benpuan12MouseClicked

    private void benpuan13MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan13MouseClicked
        if (skor[12] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan13.getText()), 12);
        }
    }//GEN-LAST:event_benpuan13MouseClicked

    private void benpuan14MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benpuan14MouseClicked
        if (skor[13] == -1 && islemSirasi > 2 && islemSirasi < 6) {
            puani_ekle(Integer.parseInt(benpuan14.getText()), 13);
        }
    }//GEN-LAST:event_benpuan14MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Yahtzee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Yahtzee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Yahtzee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Yahtzee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Yahtzee().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton baglanbutton;
    private javax.swing.JLabel benpuan1;
    private javax.swing.JLabel benpuan10;
    private javax.swing.JLabel benpuan11;
    private javax.swing.JLabel benpuan12;
    private javax.swing.JLabel benpuan13;
    private javax.swing.JLabel benpuan14;
    private javax.swing.JLabel benpuan15;
    private javax.swing.JLabel benpuan16;
    private javax.swing.JLabel benpuan2;
    private javax.swing.JLabel benpuan3;
    private javax.swing.JLabel benpuan4;
    private javax.swing.JLabel benpuan5;
    private javax.swing.JLabel benpuan6;
    private javax.swing.JLabel benpuan7;
    private javax.swing.JLabel benpuan8;
    private javax.swing.JLabel benpuan9;
    private javax.swing.JLabel benzar1;
    private javax.swing.JLabel benzar2;
    private javax.swing.JLabel benzar3;
    private javax.swing.JLabel benzar4;
    private javax.swing.JLabel benzar5;
    private javax.swing.JPanel bizimPanel;
    public javax.swing.JLabel durum;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel masaPanel;
    private javax.swing.JLabel masazar1;
    private javax.swing.JLabel masazar2;
    private javax.swing.JLabel masazar3;
    private javax.swing.JLabel masazar4;
    private javax.swing.JLabel masazar5;
    private javax.swing.JPanel rakipPanel;
    private javax.swing.JLabel rakippuan1;
    private javax.swing.JLabel rakippuan10;
    private javax.swing.JLabel rakippuan11;
    private javax.swing.JLabel rakippuan12;
    private javax.swing.JLabel rakippuan13;
    private javax.swing.JLabel rakippuan14;
    private javax.swing.JLabel rakippuan15;
    private javax.swing.JLabel rakippuan16;
    private javax.swing.JLabel rakippuan2;
    private javax.swing.JLabel rakippuan3;
    private javax.swing.JLabel rakippuan4;
    private javax.swing.JLabel rakippuan5;
    private javax.swing.JLabel rakippuan6;
    private javax.swing.JLabel rakippuan7;
    private javax.swing.JLabel rakippuan8;
    private javax.swing.JLabel rakippuan9;
    private javax.swing.JLabel rakipzar1;
    private javax.swing.JLabel rakipzar2;
    private javax.swing.JLabel rakipzar3;
    private javax.swing.JLabel rakipzar4;
    private javax.swing.JLabel rakipzar5;
    private javax.swing.JButton zarla;
    // End of variables declaration//GEN-END:variables
}
