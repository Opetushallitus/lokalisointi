import React from "react"
import {Box, CircularProgress} from "@mui/material"

export const Loading: React.FC = () => {
  return (
    <Box sx={{display: 'flex'}}>
      <CircularProgress/>
    </Box>
  )
}