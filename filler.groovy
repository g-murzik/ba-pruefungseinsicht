@Grab('org.slf4j:slf4j-api:1.7.13')
@Grab('com.itextpdf:kernel:7.0.0')
@Grab('com.itextpdf:io:7.0.0')
@Grab('com.itextpdf:layout:7.0.0')

import java.text.SimpleDateFormat

import com.itextpdf.io.font.FontConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas

class Adresse
{
    String strasse
    String hausnummer
    String plz
    String stadt

    void setZipCode(String plz)
    {
        assert plz ==~ /\d{5}/
        this.plz = plz
    }

    Adresse(Properties p)
    {
        strasse     = p.strasse
        hausnummer  = p.hausnummer
        stadt       = p.stadt
        setZipCode    p.plz
    }

    @Override
    String toString(){ return "$strasse $hausnummer $plz $stadt"}
}

class Filler
{
    final String FORM_PATH = 'form.pdf'

    String  studiengang
    String  seminargruppe
    String  nachname
    String  vorname
    Adresse adresse
    String  semester

    Filler(Properties p)
    {
        studiengang     = p.studiengang
        seminargruppe   = p.seminargruppe
        vorname         = p.vorname
        nachname        = p.nachname
        semester        = p.semester

        adresse         = new Adresse(p)
    }

    void init()
    {
        File form = new File(FORM_PATH)
        if (form.exists())
            return

        def fos = new FileOutputStream(form)
        def bos = new BufferedOutputStream(fos)
        def url = new URL('http://www.ba-dresden.de/de/service/dokumente/dokumente-fuer-alle-studierenden.html?eID=dam_frontend_push&docID=5047')
        bos << url.openStream()
        bos.close()
    }

    void fill(String dest, String modulID, String pruefungsteile, String bestanden) throws FileNotFoundException, IOException
    {
        init()

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(FORM_PATH), new PdfWriter(dest))
        PdfFont font = PdfFontFactory.createFont FontConstants.TIMES_ROMAN
        PdfCanvas pdfCanvas = new PdfCanvas(pdfDocument.getFirstPage())

        pdfCanvas.setFontAndSize(font, 12)

        def entries = []
        entries << [140, 665, studiengang]
        entries << [475, 665, seminargruppe]
        entries << [110, 642, nachname]
        entries << [350, 642, vorname]
        entries << [220, 619, adresse.toString()]
        entries << [ 85, 395, semester]
        entries << [130, 395, modulID]
        entries << [350, 395, pruefungsteile]
        entries << [450, 395, bestanden]
        entries << [110, 349, (new SimpleDateFormat("dd.MM.yyyy")).format(Calendar.getInstance().getTime()).toString()]

        entries.each { x, y, text ->
            pdfCanvas.setTextMatrix(x, y)
            pdfCanvas.showText(text)
        }

        pdfCanvas.endText()
        pdfCanvas.fill()
        pdfCanvas.release()

        pdfDocument.close()
    }
}

Properties getPropertiesFromPath(String path)
{
    Properties properties = new Properties()
    File propertiesFile = new File(path)
    propertiesFile.withReader'UTF8', { properties.load(it) }

    return properties
}

static void main(String[] args)
{
    def modulID        = System.getProperty('modulID') ?: ''
    def pruefungsteile = System.getProperty('pruefungsteile') ?: ''
    def bestanden      = System.getProperty('bestanden') ?: ''

    if (args =~ /-?-?h(elp)?/)
    {
        println '''usage:
        <script_name> [-DmodulID="12345"] [-Dpruefungsteile="1"] [-Dbestanden="ja"]'''
    }

    Filler filler = new Filler(getPropertiesFromPath('form.properties'))
    filler.fill 'output.pdf', modulID, pruefungsteile, bestanden
}