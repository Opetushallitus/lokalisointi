import React, {useState} from "react"
import {Button, Container, Grid2 as Grid, Paper, Snackbar} from "@mui/material"
import Typography from "@mui/material/Typography"
import OpenInNewIcon from "@mui/icons-material/OpenInNew"
import {DevBackend, Tolgee} from "@tolgee/web"
import {BackendFetch, FormatSimple, T, TolgeeProvider} from "@tolgee/react"
import {v4 as uuidv4} from "uuid"
import {OphThemeProvider} from "@opetushallitus/oph-design-system/theme"
import {UiVirkailijaRaamit} from "./UIVirkailijaRaamit"
import {CopyLocalisations} from "./CopyLocalisations"
import {LocalisationOverrides} from "./LocalisationOverrides"
import {Loading} from "./Loading"
import {Message, UIConfig} from "./types"

interface UIProps {
  lang: "fi" | "sv" | "en"
  uiConfig: UIConfig
}

export const UI: React.FC<UIProps> = ({lang, uiConfig}) => {
  const [messages, setMessages] = useState<Message[]>([])
  const deleteMessage = (id: string) => {
    return () =>
      setMessages((msgs) => msgs.filter(m => m.id !== id))
  }
  const showMessage = (message: React.ReactNode) => {
    setMessages((msgs) => [...msgs, {message, id: uuidv4()}])
  }
  const tolgeeChain = Tolgee()
    .use(FormatSimple())
    .use(BackendFetch({
      // this can point to OPH CloudFront once configured
      prefix: "https://cdn.tolg.ee/757120c6367d7d321f780779868b1802"
    }))
  if (uiConfig.currentEnvironment === "pallero") {
    tolgeeChain.use(DevBackend())
  }

  const tolgee = tolgeeChain
    .init({
      language: lang,
      availableLanguages: ["fi", "sv", "en"],
      defaultNs: "lokalisointi",
      ns: ["lokalisointi"],
      projectId: 11100
    })
  return (
    <TolgeeProvider
      tolgee={tolgee}
      fallback={<Loading/>}
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