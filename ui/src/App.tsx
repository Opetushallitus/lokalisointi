import React, {useEffect, useState} from "react"
import Typography from "@mui/material/Typography"
import {Button, Container, Grid2 as Grid, Snackbar} from "@mui/material"
import OpenInNewIcon from "@mui/icons-material/OpenInNew"
import CopyLocalisations from "./CopyLocalisations"
import LocalisationOverrides from "./LocalisationOverrides"
import {Message, UIConfig} from "./types"
import {v4 as uuidv4} from "uuid"
import {OphThemeProvider} from "@opetushallitus/oph-design-system/theme"
import dayjs from "dayjs"
import LocalizedFormat from "dayjs/plugin/localizedFormat"
import {Tolgee, BackendFetch, FormatSimple, TolgeeProvider, T} from "@tolgee/react"

require("dayjs/locale/fi")

dayjs.locale("fi")
dayjs.extend(LocalizedFormat)

const tolgee = Tolgee()
  .use(FormatSimple())
  .use(BackendFetch({prefix: "https://cdn.tolg.ee/757120c6367d7d321f780779868b1802"}))
  .init({
    language: "fi",
    availableLanguages: ["fi", "sv", "en"],
    defaultNs: "lokalisointi",
    ns: ["lokalisointi"]
  })

export default function App() {
  const [uiConfig, setUiConfig] = useState<UIConfig>({})
  const [messages, setMessages] = useState<Message[]>([])
  const [lang, setLang] = useState<"fi" | "sv" | "en">("fi")
  const [raamitUrl, setRaamitUrl] = useState<string | undefined>(undefined)
  useEffect(() => {
    fetch("/lokalisointi/api/v1/ui-config").then(async response => {
      if (response.ok) {
        setUiConfig(await response.json())
      }
    })
  }, [])
  useEffect(() => {
    if (uiConfig.virkalijaBaseUrl) {
      setRaamitUrl(`${uiConfig.virkalijaBaseUrl}/virkailija-raamit/apply-raamit.js`)
      fetch(`${uiConfig.virkalijaBaseUrl}/kayttooikeus-service/cas/me`).then(async response => {
        if (response.ok) {
          const me = await response.json()
          if (me.lang) {
            setLang(me.lang)
            await tolgee.changeLanguage(me.lang)
          }
        }
      })
    }
  }, [uiConfig.virkalijaBaseUrl]);
  const deleteMessage = (id: string) => {
    return () =>
      setMessages((msgs) => msgs.filter(m => m.id !== id))
  }
  const showMessage = (message: React.ReactNode) => {
    setMessages((msgs) => [...msgs, {message, id: uuidv4()}])
  }

  return (
    <TolgeeProvider
      tolgee={tolgee}
      fallback="..."
    >
      <OphThemeProvider variant="oph" lang={lang}>
        <Container maxWidth="xl">
          <Grid container spacing={3}>
            <Grid size={12}>
              <Typography variant="h4"><T keyName="main-title" defaultValue="Käännösten hallinta"/></Typography>
            </Grid>
            <Grid size={8}>
              <Typography
                variant="body1"><T keyName="general-info-text"
                                   defaultValue="Käännöksiä lisätään, muokataan ja poistetaan Tolgeessa, ulkoisessa käännöstenhallintapalvelussa. Tolgee julkaisee käännöstiedostot testiympäristöön, josta ne kopioidaan muihin ympäristöihin alla olevan kopiointitoiminnon avulla."/></Typography>
            </Grid>
            <Grid size={4}>
              <Button href="https://app.tolgee.io" variant="contained" color="primary" fullWidth
                      target="_blank" endIcon={<OpenInNewIcon/>}>
                <T keyName="edit-translations" defaultValue="Muokkaa käännöksiä"/>
              </Button>
            </Grid>
            <Grid size={12}>
              <CopyLocalisations uiConfig={uiConfig} showMessage={showMessage}/>
            </Grid>
            <Grid size={12}>
              <LocalisationOverrides showMessage={showMessage}/>
            </Grid>
          </Grid>
          {messages.map((message, i) => (
            <Snackbar
              open={!!messages.find(m => m.id === message.id)}
              autoHideDuration={5000}
              onClose={deleteMessage(message.id)}
              message={message.message}
              key={i}
            />
          ))}
        </Container>
        {!!raamitUrl && <script src={raamitUrl}/>}
      </OphThemeProvider>
    </TolgeeProvider>
  )
}
