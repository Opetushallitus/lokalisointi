import React, {useEffect, useState} from "react"
import Typography from "@mui/material/Typography"
import {Button, Container, Grid2 as Grid, Paper, Snackbar} from "@mui/material"
import OpenInNewIcon from "@mui/icons-material/OpenInNew"
import CopyLocalisations from "./CopyLocalisations"
import LocalisationOverrides from "./LocalisationOverrides"
import {Message, UIConfig} from "./types"
import {v4 as uuidv4} from "uuid"
import {OphThemeProvider} from "@opetushallitus/oph-design-system/theme"
import dayjs from "dayjs"
import LocalizedFormat from "dayjs/plugin/localizedFormat"
import {Tolgee, BackendFetch, FormatSimple, TolgeeProvider, T} from "@tolgee/react"
import {UiVirkailijaRaamit} from "./UIVirkailijaRaamit"

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
  useEffect(() => {
    fetch("/lokalisointi/api/v1/ui-config").then(async response => {
      if (response.ok) {
        setUiConfig(await response.json())
      }
    })
    fetch(`/kayttooikeus-service/cas/me`).then(async response => {
      if (response.ok) {
        const me = await response.json()
        if (me.lang) {
          setLang(me.lang)
          await tolgee.changeLanguage(me.lang)
        }
      }
    })
  }, [])
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
        <UiVirkailijaRaamit scriptUrl="/virkailija-raamit/apply-raamit.js"/>
        <Container maxWidth="xl" sx={theme => ({mt: theme.spacing(4)})}>
          <Paper elevation={0} sx={theme => ({p: theme.spacing(4)})}>
            <Grid container spacing={3}>
              <Grid size={12}>
                <Typography variant="h2"><T keyName="main-title" defaultValue="Käännösten hallinta"/></Typography>
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
            </Grid>
          </Paper>
          <Paper elevation={0} sx={theme => ({p: theme.spacing(4), mt: theme.spacing(4)})}>
            <CopyLocalisations uiConfig={uiConfig} showMessage={showMessage}/>
          </Paper>
          <Paper elevation={0} sx={theme => ({p: theme.spacing(4), mt: theme.spacing(4)})}>
            <LocalisationOverrides showMessage={showMessage}/>
          </Paper>
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
      </OphThemeProvider>
    </TolgeeProvider>
  )
}
