import {useEffect, useState} from "react"
import Typography from "@mui/material/Typography"
import {Button, Container, Grid2 as Grid, Snackbar} from "@mui/material"
import OpenInNewIcon from "@mui/icons-material/OpenInNew"
import CopyLocalisations from "./CopyLocalisations"
import LocalisationOverrides from "./LocalisationOverrides"
import {Message, UIConfig} from "./types"
import {v4 as uuidv4} from "uuid"

export default function App() {
  const [uiConfig, setUiConfig] = useState<UIConfig>({})
  const [messages, setMessages] = useState<Message[]>([])
  useEffect(() => {
    fetch("/lokalisointi/api/v1/ui-config").then(res => res.json()).then(res => {
      setUiConfig(res)
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
    <Container maxWidth="xl">
      <Grid container spacing={3}>
        <Grid size={12}>
          <Typography variant="h4">Käännösten hallinta</Typography>
        </Grid>
        <Grid size={8}>
          <Typography variant="body1">Käännöksiä lisätään, muokataan ja poistetaan Tolgeessa, ulkoisessa
            käännöstenhallintapalvelussa. Tolgee julkaisee käännöstiedostot testiympäristöön, josta ne kopioidaan muihin
            ympäristöihin alla olevan kopiointitoiminnon avulla.</Typography>
        </Grid>
        <Grid size={4}>
          <Button variant="contained" href="https://app.tolgee.io" target="_blank" endIcon={<OpenInNewIcon/>} fullWidth>
            Muokkaa käännöksiä
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
  )
}
