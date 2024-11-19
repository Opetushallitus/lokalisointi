import React, {useState} from "react"
import {
  FormControl,
  FormHelperText,
  FormLabel,
  Grid2 as Grid,
  MenuItem,
  Select
} from "@mui/material"
import Typography from "@mui/material/Typography"
import {UIConfig} from "./types"
import {OphButton, OphSelect} from "@opetushallitus/oph-design-system"
import {useTranslate} from "@tolgee/react"

interface Props {
  uiConfig: UIConfig,
  showMessage: (message: React.ReactNode) => void
}

export const CopyLocalisations: React.FC<Props> = ({uiConfig, showMessage}) => {
  const [source, setSource] = useState<string>("")
  const [availableNamespaces, setAvailableNamespaces] = useState<string[]>([])
  const [namespaces, setNamespaces] = useState<string[]>([])
  const [response, setResponse] = useState<string>("")
  const {t: translate} = useTranslate()
  const loadAvailableNamespaces = (sourceEnvironment: string) => {
    fetch(`/lokalisointi/api/v1/copy/available-namespaces?source=${sourceEnvironment}`, {
      method: "GET"
    }).then(async (res) => {
      const body = await res.json()
      if (!res.ok) {
        showMessage(translate("namespaces-could-not-be-loaded", "Nimiavaruuksia ei saatu ladattua. Yritä myöhemmin uudelleen."))
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
        showMessage(translate("copy-translations-failed", {
          body: JSON.stringify(body)
        }))
        return
      }
      setResponse(JSON.stringify(res))
    })
  }
  return (
    <Grid container spacing={3}>
      <Grid size={12}>
        <Typography
          variant="h2">{translate("copy-translations-title", "Käännösten kopiointi ympäristöstä toiseen")}</Typography>
      </Grid>
      <Grid size={4}>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="copy-source">{translate("copy-source", "lähdeympäristö")}</FormLabel>
          <OphSelect id="copy-source" value={source} size="small"
                     onChange={(e) => {
                       const value = e.target.value
                       setSource(value)
                       loadAvailableNamespaces(value)
                     }}
                     options={uiConfig.sourceEnvironments?.map(
                       (environment, _) => ({
                         label: translate(`env-${environment}`, environment),
                         value: environment
                       })
                     ) ?? []}/>
          <FormHelperText>{translate("copy-source-help", "käännökset kopioidaan lähdeympäristöstä ympäristöön {target}", {
            target: !!uiConfig.currentEnvironment ? translate(`env-${uiConfig.currentEnvironment}`, uiConfig.currentEnvironment) : "-"
          })}</FormHelperText>
        </FormControl>
      </Grid>
      <Grid size={4}>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="copy-namespaces">{translate("copy-namespaces", "kopioitavat nimiavaruudet")}</FormLabel>
          <Select id="copy-namespaces" variant="outlined" value={namespaces} size="small"
                  onChange={(e) => setNamespaces(e.target.value as string[])}
                  multiple disabled={availableNamespaces.length === 0}>
            {availableNamespaces.map((
              (ns, i) => <MenuItem value={ns} key={i}>{ns ?? '<none>'}</MenuItem>)
            )}
          </Select>
          <FormHelperText>{translate("copy-namespaces-help", "jätä valitsematta mitään jos kopioidaan kaikki nimiavaruudet")}</FormHelperText>
        </FormControl>
      </Grid>
      <Grid size={4}>
        <OphButton fullWidth variant="contained" onClick={copy} color="primary" disabled={!source}
                   sx={(theme) => ({mt: theme.spacing(3)})}>{translate("copy", "Kopioi")}</OphButton>
      </Grid>
      <Grid size={12}>
        {!!response && <Typography variant="body1">{response}</Typography>}
      </Grid>
    </Grid>
  )
}
