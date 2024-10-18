import {useEffect, useState} from "react"
import {Button, FormControl, FormLabel, Grid, Input, MenuItem, Select} from "@mui/material"

export default function AddOverride({added, uiConfig}) {
  const [availableNamespaces, setAvailableNamespaces] = useState([])
  const [namespace, setNamespace] = useState("")
  const [key, setKey] = useState("")
  const [locale, setLocale] = useState("")
  const [value, setValue] = useState("")
  useEffect(() => {
    if (uiConfig.currentEnvironment) {
      fetch(`/lokalisointi/api/v1/copy/available-namespaces?source=${uiConfig.currentEnvironment}`, {
        method: "GET"
      }).then(res => res.json()).then(res => setAvailableNamespaces(res))
    }
  }, [uiConfig.currentEnvironment])
  const save = () => {
    fetch("/lokalisointi/api/v1/override", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify({namespace, locale, key, value})
    })
      .then(res => res.json())
      .then(_ => {
        setNamespace("")
        setLocale("")
        setKey("")
        setValue("")
        added()
      })
  }
  return (
    <Grid container>
      <Grid item xs={3}>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="namespace">Namespace</FormLabel>
          <Select id="namespace" variant="filled" value={namespace}
                  onChange={(e) => setNamespace(e.target.value)}>
            {availableNamespaces.map((
              (ns, i) => <MenuItem value={ns} key={i}>{ns}</MenuItem>)
            )}
          </Select>
        </FormControl>
      </Grid>
      <Grid item xs={3}>
        <FormControl fullWidth>
          <FormLabel htmlFor="locale">Locale</FormLabel>
          <Select id="locale" variant="filled" value={locale}
                  onChange={(e) => setLocale(e.target.value)}>
            <MenuItem value="fi">suomi</MenuItem>
            <MenuItem value="sv">ruotsi</MenuItem>
            <MenuItem value="en">englanti</MenuItem>
          </Select>
        </FormControl>
      </Grid>
      <Grid item xs={3}>
        <FormControl fullWidth>
          <FormLabel htmlFor="key">Key</FormLabel>
          <Input id="key" value={key} onChange={(e) => setKey(e.target.value)}/>
        </FormControl>
      </Grid>
      <Grid item xs={3}>
        <FormControl fullWidth>
          <FormLabel htmlFor="value">Value</FormLabel>
          <Input id="value" value={value} onChange={(e) => setValue(e.target.value)}/>
        </FormControl>
      </Grid>
      <Grid item xs={12}>
        <Button variant="contained" onClick={save} disabled={!namespace || !key || !locale || !value}>Tallenna</Button>
      </Grid>
    </Grid>
  )
}