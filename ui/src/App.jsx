import Typography from '@mui/material/Typography'
import {Button, Container, Grid, styled} from "@mui/material"
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import CopyLocalisations from "./CopyLocalisations"
import LocalisationOverrides from "./LocalisationOverrides"
import {useEffect, useState} from "react"

const SpacedButton = styled(Button)(({theme}) => ({
  marginTop: theme.spacing(2)
}))

export default function App() {
  const [uiConfig, setUiConfig] = useState({})
  useEffect(() => {
    fetch("/lokalisointi/api/v1/ui-config").then(res => res.json()).then(res => {
      setUiConfig(res)
    })
  }, [])
  return (
    <Container maxWidth="xl">
      <Grid container spacing={4}>
        <Grid item xs={12}>
          <Typography variant="h4">Käännösten hallinta</Typography>
          <Typography variant="body1">Käännöksiä lisätään, muokataan ja poistetaan Tolgeessa, ulkoisessa
            käännöstenhallintapalvelussa. Tolgee julkaisee käännöstiedostot testiympäristöön, josta ne kopioidaan muihin
            ympäristöihin alempana olevan kopiointitoiminnon avulla.</Typography>
          <SpacedButton variant="contained" href="https://app.tolgee.io" target="_blank" endIcon={<OpenInNewIcon/>}>
            Muokkaa käännöksiä
          </SpacedButton>
        </Grid>
        <Grid item xs={12}>
          <CopyLocalisations uiConfig={uiConfig}/>
        </Grid>
        <Grid item xs={12}>
          <LocalisationOverrides uiConfig={uiConfig}/>
        </Grid>
      </Grid>
    </Container>
  )
}
