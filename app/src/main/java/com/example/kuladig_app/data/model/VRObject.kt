package com.example.kuladig_app.data.model

data class VRObject(
    val id: String,
    val title: String,
    val glbFileName: String,
    val description: String
) {
    companion object {
        fun getAllObjects(): List<VRObject> = listOf(
            VRObject(
                id = "caesar",
                title = "Cäsar-Statue",
                glbFileName = "cesar.glb",
                description = "Die Cäsar-Statue im Louvre zeigt Gaius Julius Caesar, den berühmten römischen Feldherrn und Staatsmann. Sie stammt aus der römischen Kaiserzeit (1. Jh. v. Chr./n. Chr.) und ist für ihren realistischen Stil bekannt: markante Gesichtszüge, hoher Haaransatz und ein ernsthafter Ausdruck. Die Statue betont Cäsars Macht, Autorität und politische Bedeutung und gilt als eines der bekanntesten Porträts des römischen Altertums."
            ),
            VRObject(
                id = "munze",
                title = "Münze",
                glbFileName = "munze.glb",
                description = "Eine Münze des Österreichisch-Ungarischen Reiches stammt aus der Zeit 1867–1918. Sie zeigt meist das Porträt von Kaiser Franz Joseph I. oder das kaiserliche Doppeladler-Wappen. Die Münzen wurden aus Silber, Gold oder Kupfer geprägt und dienten als offizielles Zahlungsmittel der Donaumonarchie. Sie stehen heute als historische Zeugnisse für die politische Macht und Vielvölkerstruktur des Reiches."
            )
        )
    }
}