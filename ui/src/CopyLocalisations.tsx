import React, {useState, FC} from "react"
import {
  Button,
  FormControl,
  FormHelperText,
  FormLabel,
  Grid2 as Grid,
  MenuItem,
  Select
} from "@mui/material"
import Typography from "@mui/material/Typography"
import {UIConfig} from "./types"

interface Props {
  uiConfig?: UIConfig,
  showMessage: (message: React.ReactNode) => void
}

const CopyLocalisations: FC<Props> = ({uiConfig, showMessage}) => {
  const [source, setSource] = useState<string>("")
  const [availableNamespaces, setAvailableNamespaces] = useState<string[]>([])
  const [namespaces, setNamespaces] = useState<string[]>([])
  const [response, setResponse] = useState<string>("")
  const envNames: Map<string, string> = new Map(Object.entries({
    "pallero": "Testi (master data)",
    "untuva": "Untuva",
    "hahtuva": "Hahtuva",
    "sade": "Tuotanto"
  }))

  const loadAvailableNamespaces = (sourceEnvironment: string) => {
    fetch(`/lokalisointi/api/v1/copy/available-namespaces?source=${sourceEnvironment}`, {
      method: "GET"
    }).then(async (res) => {
      const body = await res.json()
      if (!res.ok) {
        showMessage("Nimiavaruuksia ei saatu ladattua. Yritä myöhemmin uudelleen.")
        return
      }
      setAvailableNamespaces(body)
    }).catch(err => console.log(`Error fetcing available namespaces: ${err}`))
  }

  const copy = () => {
    fetch("/lokalisointi/api/v1/copy", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify({source: source, namespaces: namespaces})
    }).then(async (res) => {
      const body = await res.json()
      if (!res.ok) {
        showMessage(`Käännösten kopioiminen ei onnistunut: ${JSON.stringify(body)}`)
        return
      }
      setResponse(JSON.stringify(res))
    })
  }
  return (
    <Grid container spacing={3}>
      <Grid size={12}>
        <Typography variant="h4">Käännösten kopiointi ympäristöstä toiseen</Typography>
      </Grid>
      <Grid size={4}>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="copy-source">lähdeympäristö</FormLabel>
          <Select id="copy-source" variant="outlined" value={source} size="small"
                  onChange={(e) => {
                    const value = e.target.value
                    setSource(value)
                    loadAvailableNamespaces(value)
                  }}>
            {uiConfig?.sourceEnvironments?.map(
              (environment, i) => <MenuItem value={environment} key={i}>{envNames.get(environment)}</MenuItem>
            )}
          </Select>
          <FormHelperText>käännökset kopioidaan lähdeympäristöstä ympäristöön&nbsp;
            {envNames.get(uiConfig!.currentEnvironment!)}</FormHelperText>
        </FormControl>
      </Grid>
      <Grid size={4}>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="copy-namespaces">kopioitavat nimiavaruudet</FormLabel>
          <Select id="copy-namespaces" variant="outlined" value={namespaces} size="small"
                  onChange={(e) => setNamespaces(e.target.value as string[])}
                  multiple disabled={availableNamespaces.length === 0}>
            {availableNamespaces.map((
              (ns, i) => <MenuItem value={ns} key={i}>{ns}</MenuItem>)
            )}
          </Select>
          <FormHelperText>jätä valitsematta mitään jos kopioidaan kaikki nimiavaruudet</FormHelperText>
        </FormControl>
      </Grid>
      <Grid size={4}>
        <Button fullWidth variant="contained" onClick={copy} color="primary" disabled={!source}
                sx={(theme) => ({mt: theme.spacing(3)})}>Kopioi</Button>
      </Grid>
      <Grid size={12}>
        {!!response && <Typography variant="body1">{response}</Typography>}
      </Grid>
    </Grid>
  )
}

export default CopyLocalisations