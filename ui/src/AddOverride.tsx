import React, {ChangeEvent, FC, useEffect, useState} from "react"
import {
  Autocomplete,
  FormControl,
  FormLabel,
  IconButton,
  MenuItem,
  Select,
  TableCell,
  TableRow,
  TextField,
  Tooltip
} from "@mui/material"
import {Cancel, Save} from "@mui/icons-material"

interface Props {
  close: () => void,
  showMessage: (message: React.ReactNode) => void
}

const AddOverride: FC<Props> = ({close, showMessage}) => {
  const [availableNamespaces, setAvailableNamespaces] = useState<string[]>([])
  const [namespace, setNamespace] = useState<string | undefined>(undefined)
  const [key, setKey] = useState<string>("")
  const [locale, setLocale] = useState<string>("")
  const [value, setValue] = useState<string>("")
  useEffect(() => {
    if (showMessage) {
      fetch("/lokalisointi/api/v1/override/available-namespaces", {
        method: "GET"
      }).then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          showMessage("Nimiavaruuksia ei saatu ladattua. Yritä myöhemmin uudelleen.")
          return
        }
        setAvailableNamespaces(body)
      })
    }
  }, [showMessage])
  const save = () => {
    fetch("/lokalisointi/api/v1/override", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify({namespace: namespace, locale: locale, key: key, value: value})
    })
      .then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          showMessage(`Yliajon tallentaminen ei onnistunut: ${JSON.stringify(body)}`)
          return
        }
        setNamespace(undefined)
        setKey("")
        setLocale("")
        setValue("")
        close()
      })
  }
  return (
    <TableRow>
      <TableCell></TableCell>
      <TableCell>
        <FormControl variant="filled" fullWidth>
          <FormLabel htmlFor="namespace">nimiavaruus</FormLabel>
          <Autocomplete
            id="namespace"
            freeSolo
            disableClearable
            value={namespace}
            onChange={(_, value) => {
              setNamespace(value)
            }}
            options={availableNamespaces.map((option) => option)}
            renderInput={(params) => (
              <TextField
                {...params}
                variant="outlined"
                size="small"
                onChange={(e) => {
                  setNamespace(e.target.value)
                }}
                slotProps={{
                  input: {
                    ...params.InputProps,
                    type: 'search',
                  },
                }}
              />
            )}
          />
        </FormControl>
      </TableCell>
      <TableCell>
        <FormControl fullWidth>
          <FormLabel htmlFor="key">avain</FormLabel>
          <TextField id="key" variant="outlined" size="small" value={key}
                     onChange={(e) => setKey(e.target.value)}/>
        </FormControl>
      </TableCell>
      <TableCell>
        <FormControl fullWidth>
          <FormLabel htmlFor="locale">kieli</FormLabel>
          <Select id="locale" variant="outlined" value={locale} size="small"
                  onChange={(e) => setLocale(e.target.value)}>
            <MenuItem value="fi">fi</MenuItem>
            <MenuItem value="sv">sv</MenuItem>
            <MenuItem value="en">en</MenuItem>
          </Select>
        </FormControl>
      </TableCell>
      <TableCell>
        <FormControl fullWidth>
          <FormLabel htmlFor="value">arvo</FormLabel>
          <TextField id="value" value={value} multiline size="small" variant="outlined"
                     onChange={(e: ChangeEvent<HTMLTextAreaElement>) =>
                       setValue(e.target.value)}/>
        </FormControl>
      </TableCell>
      <TableCell colSpan={5} sx={{verticalAlign: "bottom"}}>
        <Tooltip title="tallenna">
          <IconButton onClick={save} disabled={!namespace || !key || !locale || !value}
                      color="primary"><Save/></IconButton>
        </Tooltip>
        <Tooltip title="peruuta">
          <IconButton onClick={close}><Cancel/></IconButton>
        </Tooltip>
      </TableCell>
    </TableRow>
  )
}

export default AddOverride