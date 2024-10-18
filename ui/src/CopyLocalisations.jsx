import {useEffect, useState} from "react"
import {Button, FormControl, FormHelperText, Grid, InputLabel, MenuItem, Select} from "@mui/material"
import Typography from "@mui/material/Typography"

export default function CopyLocalisations({uiConfig}) {
  const [source, setSource] = useState("")
  const [availableNamespaces, setAvailableNamespaces] = useState([])
  const [namespaces, setNamespaces] = useState([])
  const [response, setResponse] = useState("")
  const envNames = {
    pallero: "Testi (masterdata)",
    untuva: "Untuva",
    hahtuva: "Hahtuva",
    sade: "Tuotanto"
  }

  useEffect(() => {
    if (source) {
      fetch(`/lokalisointi/api/v1/copy/available-namespaces?source=${source}`, {
        method: "GET"
      }).then(res => res.json()).then(res => setAvailableNamespaces(res))
    }
  }, [source])

  const copy = () => {
    fetch("/lokalisointi/api/v1/copy", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify({source: source, namespaces: namespaces})
    }).then(res => res.json()).then(res => setResponse(JSON.stringify(res)))
  }
  return (
    <Grid container spacing={2}>
      <Grid item xs={12}>
        <Typography variant="h4">Käännösten kopiointi</Typography>
      </Grid>
      <Grid item xs={6}>
        <FormControl variant="filled" fullWidth>
          <InputLabel htmlFor="copy-source">Kopioi käännökset lähdeympäristöstä</InputLabel>
          <Select id="copy-source" variant="filled" value={source}
                  onChange={(e) => setSource(e.target.value)}>
            {!!uiConfig && (uiConfig.sourceEnvironments || []).map(
              (environment, i) => <MenuItem value={environment} key={i}>{envNames[environment]}</MenuItem>
            )}
          </Select>
          <FormHelperText>käännöstiedostot kopioidaan lähdeympäristöstä tähän ympäristöön
            ({envNames[!!uiConfig && uiConfig.currentEnvironment || 'pallero']})</FormHelperText>
        </FormControl>
      </Grid>
      <Grid item xs={6}>
        <FormControl variant="filled" fullWidth>
          <InputLabel htmlFor="copy-namespaces">kopioitavat nimiavaruudet</InputLabel>
          <Select id="copy-namespaces" variant="filled" value={namespaces}
                  onChange={(e) => setNamespaces(e.target.value)}
                  multiple rows={8} disabled={availableNamespaces.length === 0}>
            {availableNamespaces.map((
              (ns, i) => <MenuItem value={ns} key={i}>{ns}</MenuItem>)
            )}
          </Select>
          <FormHelperText>jätä valitsematta mitään jos kopioidaan kaikki nimiavaruudet</FormHelperText>
        </FormControl>
      </Grid>
      <Grid item xs={12}>
        <Button fullWidth variant="contained" onClick={copy}>Kopioi</Button>
      </Grid>
      <Grid item xs={12}>
        {!!response && <Typography variant="body1">{response}</Typography>}
      </Grid>
    </Grid>
  )
}
